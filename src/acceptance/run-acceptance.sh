#!/usr/bin/env bash
set -euo pipefail

root_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
artifact="${VHR_ARTIFACT:-$root_directory/build/libs/VelocityHotReloader-1.0.0.jar}"
velocity_api_classpath="${VHR_VELOCITY_API_CLASSPATH:-}"
velocity_version="4.1.0-SNAPSHOT"
velocity_build="9"
velocity_sha256="635ffe27b4fe1b97e61479012121d4e7c61a9eec99e6bd5a1f923053c2a259ce"
if [[ -n "${VHR_ACCEPTANCE_WORK_DIRECTORY:-}" ]]; then
    work_directory="$VHR_ACCEPTANCE_WORK_DIRECTORY"
    created_work_directory=false
else
    work_directory="$(mktemp -d)"
    created_work_directory=true
fi
keep_work_directory="${VHR_ACCEPTANCE_KEEP_WORK_DIRECTORY:-false}"
velocity_pid=""
velocity_input_fd=""

fail() { echo "VelocityHotReloader acceptance failure: $*" >&2; exit 1; }
require() { command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"; }
log_count() { grep -Ec -- "$2" "$1" 2>/dev/null || true; }

wait_for_log() {
    local file=$1 expected=$2 deadline=$((SECONDS + 120))
    while ((SECONDS < deadline)); do
        grep -Eq -- "$expected" "$file" && return
        grep -Eq 'Exception in thread|Could not load|Error occurred while enabling|VHR_ACCEPTANCE_FAIL' "$file" \
            && fail "Velocity reported a boot failure while waiting for: $expected"
        sleep 1
    done
    fail "Timed out waiting for: $expected"
}

wait_for_new_log() {
    local file=$1 expected=$2 previous_count=$3 current_count deadline=$((SECONDS + 60))
    while ((SECONDS < deadline)); do
        current_count=$(log_count "$file" "$expected")
        [[ "$current_count" -gt "$previous_count" ]] && return
        grep -Eq 'Exception in thread|Could not load|Error occurred while enabling|VHR_ACCEPTANCE_FAIL' "$file" \
            && fail "Velocity reported a runtime failure while waiting for: $expected"
        sleep 1
    done
    fail "Timed out waiting for new log entry: $expected"
}

send_command() { printf '%s\n' "$1" >&"$velocity_input_fd"; }

cleanup() {
    local exit_code=$?
    if [[ -n "$velocity_input_fd" ]]; then
        printf 'end\n' >&"$velocity_input_fd" || true
    fi
    if [[ -n "$velocity_pid" ]]; then
        local deadline=$((SECONDS + 30))
        while kill -0 "$velocity_pid" 2>/dev/null && ((SECONDS < deadline)); do sleep 1; done
        kill -0 "$velocity_pid" 2>/dev/null && kill "$velocity_pid" 2>/dev/null || true
        wait "$velocity_pid" 2>/dev/null || true
    fi
    if [[ -f "$work_directory/velocity/velocity.log" && $exit_code -ne 0 ]]; then
        tail -n 250 "$work_directory/velocity/velocity.log" >&2 || true
    fi
    if [[ "$keep_work_directory" == "true" || "$created_work_directory" != "true" ]]; then
        echo "VelocityHotReloader acceptance logs retained in $work_directory" >&2
    else
        rm -rf "$work_directory"
    fi
    exit "$exit_code"
}
trap cleanup EXIT

for command in curl java javac jar jq sha256sum; do require "$command"; done
[[ -f "$artifact" ]] || fail "Missing VHR artifact: $artifact"
[[ -n "$velocity_api_classpath" ]] || fail "Missing Velocity API compilation classpath"
mkdir -p "$work_directory/velocity/plugins" "$work_directory/sample/classes" "$work_directory/sample/resources"

metadata="$(curl --fail --silent --show-error --location \
    "https://fill.papermc.io/v3/projects/velocity/versions/$velocity_version/builds")"
runtime_url="$(jq --raw-output --argjson build "$velocity_build" \
    '.[] | select(.id == $build) | .downloads["server:default"].url' <<<"$metadata")"
[[ "$runtime_url" != "null" ]] || fail "Velocity runtime build is unavailable"
curl --fail --silent --show-error --location --output "$work_directory/velocity.jar" "$runtime_url"
[[ "$(sha256sum "$work_directory/velocity.jar" | awk '{print $1}')" == "$velocity_sha256" ]] \
    || fail "Velocity runtime checksum mismatch"

javac --release 25 -cp "$velocity_api_classpath" -d "$work_directory/sample/classes" \
    "$root_directory/src/acceptance/sample-plugin/AcceptancePlugin.java"

build_sample_plugin() {
    local destination=$1 plugin_id=$2 marker=$3 dependency=${4:-} command_enabled=$5
    rm -rf "$work_directory/sample/resources"
    mkdir -p "$work_directory/sample/resources"
    if [[ -n "$dependency" ]]; then
        printf '{"id":"%s","name":"%s","version":"%s","authors":[],"dependencies":[{"id":"%s","optional":false}],"main":"acceptance.AcceptancePlugin"}\n' \
            "$plugin_id" "$plugin_id" "$marker" "$dependency" >"$work_directory/sample/resources/velocity-plugin.json"
    else
        printf '{"id":"%s","name":"%s","version":"%s","authors":[],"main":"acceptance.AcceptancePlugin"}\n' \
            "$plugin_id" "$plugin_id" "$marker" >"$work_directory/sample/resources/velocity-plugin.json"
    fi
    printf '%s\n' "$plugin_id" >"$work_directory/sample/resources/plugin-id.txt"
    printf '%s\n' "$marker" >"$work_directory/sample/resources/marker.txt"
    printf '%s\n' "$command_enabled" >"$work_directory/sample/resources/command-enabled.txt"
    jar --create --file "$destination" -C "$work_directory/sample/classes" . -C "$work_directory/sample/resources" .
}

build_sample_plugin "$work_directory/sample/VhrAcceptanceSample.jar" "vhr-acceptance-sample" "v1" "" true
build_sample_plugin "$work_directory/sample/VhrAcceptanceConsumer.jar" "vhr-acceptance-consumer" "consumer" "vhr-acceptance-sample" false

cp "$artifact" "$work_directory/velocity/plugins/VelocityHotReloader.jar"
mkfifo "$work_directory/velocity/console.in"
(cd "$work_directory/velocity" && exec java -Xms256M -Xmx768M -jar "$work_directory/velocity.jar" <console.in >velocity.log 2>&1) &
velocity_pid=$!
exec {velocity_input_fd}>"$work_directory/velocity/console.in"
velocity_log="$work_directory/velocity/velocity.log"

wait_for_log "$velocity_log" 'Done \('
cp "$work_directory/sample/VhrAcceptanceSample.jar" "$work_directory/velocity/plugins/VhrAcceptanceSample.jar"
cp "$work_directory/sample/VhrAcceptanceConsumer.jar" "$work_directory/velocity/plugins/VhrAcceptanceConsumer.jar"

for check in \
    'vhr help|VHR Help' \
    'vhr reload|Succesvol herladen' \
    'vhr plugins --version|VHR Plugins' \
    'vhr plugininfo velocityhotreloader|VHR PluginInfo' \
    'vhr commandinfo vhr|VHR CommandInfo'; do
    command=${check%%|*}
    expected=${check#*|}
    before=$(log_count "$velocity_log" "$expected")
    send_command "$command"
    wait_for_new_log "$velocity_log" "$expected" "$before"
done

sample_enabled_before=$(log_count "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_ENABLED id=vhr-acceptance-sample marker=v1')
consumer_enabled_before=$(log_count "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_ENABLED id=vhr-acceptance-consumer marker=consumer')
send_command 'vhr loadplugin VhrAcceptanceConsumer.jar VhrAcceptanceSample.jar'
wait_for_new_log "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_ENABLED id=vhr-acceptance-sample marker=v1' "$sample_enabled_before"
wait_for_new_log "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_ENABLED id=vhr-acceptance-consumer marker=consumer' "$consumer_enabled_before"

for check in \
    'vhr plugininfo vhr-acceptance-sample|vhr-acceptance-sample' \
    'vhr commandinfo vhracceptance|vhr-acceptance-sample' \
    'vhracceptance|VHR_ACCEPTANCE_SAMPLE_COMMAND id=vhr-acceptance-sample marker=v1'; do
    command=${check%%|*}
    expected=${check#*|}
    before=$(log_count "$velocity_log" "$expected")
    send_command "$command"
    wait_for_new_log "$velocity_log" "$expected" "$before"
done

blocked_before=$(log_count "$velocity_log" 'heeft afhankelijke plugins')
send_command 'vhr unloadplugin vhr-acceptance-sample'
wait_for_new_log "$velocity_log" 'heeft afhankelijke plugins' "$blocked_before"

consumer_disabled_before=$(log_count "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_DISABLED id=vhr-acceptance-consumer')
consumer_enabled_before=$(log_count "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_ENABLED id=vhr-acceptance-consumer marker=consumer')
send_command 'vhr reloadplugin vhr-acceptance-consumer'
wait_for_new_log "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_DISABLED id=vhr-acceptance-consumer' "$consumer_disabled_before"
wait_for_new_log "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_ENABLED id=vhr-acceptance-consumer marker=consumer' "$consumer_enabled_before"

consumer_disabled_before=$(log_count "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_DISABLED id=vhr-acceptance-consumer')
sample_disabled_before=$(log_count "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_DISABLED id=vhr-acceptance-sample')
send_command 'vhr unloadplugin vhr-acceptance-consumer'
wait_for_new_log "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_DISABLED id=vhr-acceptance-consumer' "$consumer_disabled_before"
send_command 'vhr unloadplugin vhr-acceptance-sample'
wait_for_new_log "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_DISABLED id=vhr-acceptance-sample' "$sample_disabled_before"

sample_enabled_before=$(log_count "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_ENABLED id=vhr-acceptance-sample marker=v1')
send_command 'vhr loadplugin VhrAcceptanceSample.jar'
wait_for_new_log "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_ENABLED id=vhr-acceptance-sample marker=v1' "$sample_enabled_before"

watch_before=$(log_count "$velocity_log" 'Gestart met volgen')
send_command 'vhr watchplugin vhr-acceptance-sample'
wait_for_new_log "$velocity_log" 'Gestart met volgen' "$watch_before"
build_sample_plugin "$work_directory/VhrAcceptanceSample-v2.jar" "vhr-acceptance-sample" "v2" "" true
sample_disabled_before=$(log_count "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_DISABLED id=vhr-acceptance-sample')
sample_v2_enabled_before=$(log_count "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_ENABLED id=vhr-acceptance-sample marker=v2')
mv "$work_directory/VhrAcceptanceSample-v2.jar" "$work_directory/velocity/plugins/VhrAcceptanceSample.jar"
wait_for_new_log "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_DISABLED id=vhr-acceptance-sample' "$sample_disabled_before"
wait_for_new_log "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_ENABLED id=vhr-acceptance-sample marker=v2' "$sample_v2_enabled_before"

unwatch_before=$(log_count "$velocity_log" 'Gestopt met volgen')
send_command 'vhr unwatchplugin vhr-acceptance-sample'
wait_for_new_log "$velocity_log" 'Gestopt met volgen' "$unwatch_before"
sample_disabled_before=$(log_count "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_DISABLED id=vhr-acceptance-sample')
sample_v2_enabled_before=$(log_count "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_ENABLED id=vhr-acceptance-sample marker=v2')
send_command 'vhr reloadplugin vhr-acceptance-sample'
wait_for_new_log "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_DISABLED id=vhr-acceptance-sample' "$sample_disabled_before"
wait_for_new_log "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_ENABLED id=vhr-acceptance-sample marker=v2' "$sample_v2_enabled_before"

sample_disabled_before=$(log_count "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_DISABLED id=vhr-acceptance-sample')
send_command 'vhr unloadplugin vhr-acceptance-sample'
wait_for_new_log "$velocity_log" 'VHR_ACCEPTANCE_PLUGIN_DISABLED id=vhr-acceptance-sample' "$sample_disabled_before"

vhr_loaded_before=$(log_count "$velocity_log" 'Loaded plugin velocityhotreloader')
send_command 'vhr restart --force'
wait_for_new_log "$velocity_log" 'Loaded plugin velocityhotreloader' "$vhr_loaded_before"
plugins_before=$(log_count "$velocity_log" 'VHR Plugins')
send_command 'vhr plugins'
wait_for_new_log "$velocity_log" 'VHR Plugins' "$plugins_before"

send_command 'end'
deadline=$((SECONDS + 45))
while kill -0 "$velocity_pid" 2>/dev/null && ((SECONDS < deadline)); do sleep 1; done
kill -0 "$velocity_pid" 2>/dev/null && fail "Velocity did not stop cleanly"
wait "$velocity_pid" || fail "Velocity exited unsuccessfully"
velocity_pid=""
eval "exec ${velocity_input_fd}>&-"
velocity_input_fd=""

echo 'VelocityHotReloader platform acceptance passed.'
