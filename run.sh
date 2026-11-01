#!/usr/bin/env bash
# One-command launcher for spring-lab-01 on Linux.
#   ./run.sh            start PostgreSQL, rebuild if sources changed (with tests), run dev profile on :8081
#   ./run.sh prod       same, default profile on :8080
#   ./run.sh build      force clean build with tests, then run dev
#   ./run.sh test       start PostgreSQL and run the tests only
#   ./run.sh db         start PostgreSQL only
#   ./run.sh psql       open psql on the springlab database
#   ./run.sh status     show what is running
#   ./run.sh stop       stop the application and PostgreSQL
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TOOLS="$ROOT/.lab08-tools/linux"
JAR="$ROOT/target/spring-lab-01-0.0.1-SNAPSHOT.jar"
BUILD_LOG="$TOOLS/logs/build.log"
cd "$ROOT"

if [[ ! -f "$TOOLS/env.sh" ]]; then
    echo "Нет $TOOLS/env.sh — локальный PostgreSQL не найден." >&2
    exit 1
fi
# shellcheck source=/dev/null
source "$TOOLS/env.sh"

say() { printf '\033[1;36m==> %s\033[0m\n' "$*"; }
fail() { printf '\033[1;31mОШИБКА: %s\033[0m\n' "$*" >&2; exit 1; }

port_busy() { ss -ltn "sport = :$1" | grep -q LISTEN; }

check_java() {
    command -v java >/dev/null || fail "java не найдена. Установите: sudo apt install temurin-25-jdk"
    java -version 2>&1 | grep -q '"25' || fail "нужна Java 25, сейчас: $(java -version 2>&1 | head -1)"
}

STARTED_DB=0
start_db() {
    if pg_status >/dev/null 2>&1; then
        say "PostgreSQL уже запущен"
    else
        port_busy 5432 && fail "порт 5432 занят другим процессом: $(ss -ltnp 'sport = :5432' | tail -1)"
        say "Запускаю PostgreSQL (127.0.0.1:5432, база springlab)"
        pg_start >/dev/null
        STARTED_DB=1
    fi
}

needs_build() {
    [[ ! -f "$JAR" ]] && return 0
    [[ -n "$(find src pom.xml -newer "$JAR" -type f -print -quit)" ]]
}

build() {
    say "Сборка с тестами: ./mvnw clean package (полный лог: $BUILD_LOG)"
    if ./mvnw -B -ntp clean package >"$BUILD_LOG" 2>&1; then
        grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD SUCCESS' "$BUILD_LOG" | sed 's/^\[INFO\] /   /'
    else
        grep -E 'Tests run:|FAIL|ERROR' "$BUILD_LOG" | head -30
        fail "сборка не прошла, смотрите $BUILD_LOG"
    fi
}

run_app() {
    local profile="$1" port="$2"
    port_busy "$port" && fail "порт $port занят. Посмотреть: ss -ltnp | grep $port ; остановить: ./run.sh stop"
    # Stop PostgreSQL on exit only if this script started it.
    trap 'echo; if [[ $STARTED_DB == 1 ]]; then say "Останавливаю PostgreSQL"; pg_stop >/dev/null; fi' EXIT
    say "Запуск: профиль $profile, http://localhost:$port  (остановка: Ctrl+C)"
    if [[ $profile == dev ]]; then say "Лаб9: http://localhost:$port/api/lab9/evidence"; fi
    local args=()
    [[ $profile == dev ]] && args+=(--spring.profiles.active=dev)
    java -jar "$JAR" "${args[@]}"
}

MODE="${1:-dev}"
case "$MODE" in
    dev|prod)
        check_java; start_db
        if needs_build; then build; else say "Исходники не менялись — пересборка не нужна (./run.sh build — собрать заново)"; fi
        if [[ $MODE == prod ]]; then run_app default 8080; else run_app dev 8081; fi
        ;;
    build)
        check_java; start_db; build; run_app dev 8081 ;;
    test)
        check_java; start_db
        trap 'if [[ $STARTED_DB == 1 ]]; then pg_stop >/dev/null; fi' EXIT
        ./mvnw -B -ntp test ;;
    db)
        start_db; STARTED_DB=0; say "Остановить: ./run.sh stop" ;;
    psql)
        start_db; STARTED_DB=0; psql ;;
    status)
        pg_status | head -1 || true
        for p in 8080 8081; do port_busy $p && echo "Приложение слушает порт $p" || echo "Порт $p свободен"; done ;;
    stop)
        if pkill -f 'spring-lab-01-0.0.1-SNAPSHOT.jar'; then say "Приложение остановлено"; else say "Приложение не было запущено"; fi
        if pg_status >/dev/null 2>&1; then pg_stop >/dev/null; say "PostgreSQL остановлен"; else say "PostgreSQL не был запущен"; fi ;;
    -h|--help|help)
        sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//' ;;
    *)
        fail "неизвестная команда '$MODE'. Список: ./run.sh help" ;;
esac
