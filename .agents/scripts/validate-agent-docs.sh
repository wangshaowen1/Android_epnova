#!/bin/sh

set -u
LC_ALL=C
export LC_ALL

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P) || exit 2
agents_root=$(dirname "$script_dir")
repo_root=$(dirname "$agents_root")
skills_root="$agents_root/skills"
readme_path="$agents_root/README.md"

errors=0
warnings=0

relative_path() {
    case "$1" in
        "$repo_root") printf '.\n' ;;
        "$repo_root"/*) printf '%s\n' "${1#"$repo_root"/}" ;;
        *) printf '%s\n' "$1" ;;
    esac
}

add_error() {
    printf 'ERROR: %s: %s\n' "$(relative_path "$1")" "$2"
    errors=$((errors + 1))
}

add_warning() {
    printf 'WARNING: %s: %s\n' "$(relative_path "$1")" "$2"
    warnings=$((warnings + 1))
}

validate_utf8() {
    path=$1
    reject_bom=$2

    if [ "$reject_bom" = "true" ]; then
        bom=$(od -An -tx1 -N3 "$path" 2>/dev/null | tr -d '[:space:]')
        if [ "$bom" = "efbbbf" ]; then
            add_error "$path" "UTF-8 BOM found; frontmatter must start at the first byte"
        fi
    fi

    if ! awk '
        function is_continuation(value) {
            return value >= 128 && value <= 191
        }
        function invalid() {
            exit 1
        }
        BEGIN {
            for (value = 1; value <= 255; value++) {
                bytes[sprintf("%c", value)] = value
            }
        }
        {
            for (position = 1; position <= length($0); position++) {
                first = bytes[substr($0, position, 1)]
                if (first <= 127) continue

                second = bytes[substr($0, position + 1, 1)]
                if (first >= 194 && first <= 223) {
                    if (!is_continuation(second)) invalid()
                    position += 1
                    continue
                }

                third = bytes[substr($0, position + 2, 1)]
                if (first == 224) {
                    valid = second >= 160 && second <= 191 && is_continuation(third)
                } else if ((first >= 225 && first <= 236) || (first >= 238 && first <= 239)) {
                    valid = is_continuation(second) && is_continuation(third)
                } else if (first == 237) {
                    valid = second >= 128 && second <= 159 && is_continuation(third)
                } else {
                    valid = 0
                }
                if (valid) {
                    position += 2
                    continue
                }

                fourth = bytes[substr($0, position + 3, 1)]
                if (first == 240) {
                    valid = second >= 144 && second <= 191 && is_continuation(third) && is_continuation(fourth)
                } else if (first >= 241 && first <= 243) {
                    valid = is_continuation(second) && is_continuation(third) && is_continuation(fourth)
                } else if (first == 244) {
                    valid = second >= 128 && second <= 143 && is_continuation(third) && is_continuation(fourth)
                } else {
                    valid = 0
                }
                if (valid) {
                    position += 3
                    continue
                }
                invalid()
            }
        }
    ' "$path" >/dev/null 2>&1; then
        add_error "$path" "invalid UTF-8 text"
    fi
}

frontmatter_value() {
    awk -v end="$2" -v key="$3" '
        NR >= 2 && NR < end {
            sub(/\r$/, "")
            prefix = key ":"
            if (index($0, prefix) != 1) next
            value = substr($0, length(prefix) + 1)
            sub(/^[[:space:]]*/, "", value)
            sub(/[[:space:]]*$/, "", value)
            first = substr(value, 1, 1)
            last = substr(value, length(value), 1)
            if (length(value) >= 2 && first == last && (first == "\"" || first == "\047")) {
                value = substr(value, 2, length(value) - 2)
            }
            print value
            exit
        }
    ' "$1"
}

decode_uri_path() {
    printf '%s\n' "$1" | awk '
        function hex_value(char) {
            return index("0123456789ABCDEF", toupper(char)) - 1
        }
        {
            output = ""
            for (index_value = 1; index_value <= length($0); index_value++) {
                char = substr($0, index_value, 1)
                if (char == "%" && index_value + 2 <= length($0)) {
                    high = hex_value(substr($0, index_value + 1, 1))
                    low = hex_value(substr($0, index_value + 2, 1))
                    if (high >= 0 && low >= 0) {
                        output = output sprintf("%c", high * 16 + low)
                        index_value += 2
                        continue
                    }
                }
                output = output char
            }
            print output
        }
    '
}

validate_skill() {
    skill_dir=$1
    skill_path="$skill_dir/SKILL.md"

    if [ ! -f "$skill_path" ]; then
        add_error "$skill_dir" "missing SKILL.md"
        return
    fi

    validate_utf8 "$skill_path" true
    first_line=$(sed -n '1p' "$skill_path" | tr -d '\r')
    line_count=$(awk 'END { print NR }' "$skill_path")
    if [ "$line_count" -lt 4 ] || [ "$first_line" != "---" ]; then
        add_error "$skill_path" "frontmatter must start on the first line"
        return
    fi

    frontmatter_end=$(awk '
        NR > 1 {
            sub(/\r$/, "")
            if ($0 == "---") {
                print NR
                exit
            }
        }
    ' "$skill_path")
    if [ -z "$frontmatter_end" ]; then
        add_error "$skill_path" "frontmatter closing delimiter is missing"
        return
    fi

    unknown_keys=$(awk -v end="$frontmatter_end" '
        NR >= 2 && NR < end {
            sub(/\r$/, "")
            if (!match($0, /^[a-zA-Z0-9_-]+:/)) next
            key = substr($0, RSTART, RLENGTH - 1)
            if (key == "name" || key == "description") next
            if (result != "") result = result ", "
            result = result key
        }
        END { print result }
    ' "$skill_path")
    if [ -n "$unknown_keys" ]; then
        add_error "$skill_path" "frontmatter allows only name/description; found: $unknown_keys"
    fi

    name=$(frontmatter_value "$skill_path" "$frontmatter_end" "name")
    description=$(frontmatter_value "$skill_path" "$frontmatter_end" "description")
    if [ -z "$name" ]; then
        add_error "$skill_path" "name is missing"
    elif ! printf '%s\n' "$name" | grep -Eq '^[a-z0-9-]+$' || [ "${#name}" -gt 64 ]; then
        add_error "$skill_path" "name must use lowercase letters, digits, or hyphens and be at most 64 characters"
    elif [ "$name" != "$(basename "$skill_dir")" ]; then
        add_error "$skill_path" "name '$name' does not match directory '$(basename "$skill_dir")'"
    fi

    if [ -z "$description" ]; then
        add_error "$skill_path" "description is missing"
    else
        case "$description" in
            *'<'*|*'>'*) add_error "$skill_path" "description must not contain angle brackets" ;;
        esac
    fi

    if sed -n "$((frontmatter_end + 1)),\$p" "$skill_path" | tr -d '\r' | grep -Eq '^#{1,6}[[:space:]]*(触发条件|何时使用|使用时机)[[:space:]]*$'; then
        add_error "$skill_path" "trigger conditions belong in description, not a body heading"
    fi
    if [ "$line_count" -gt 500 ]; then
        add_warning "$skill_path" "$line_count lines; consider reducing or moving details to references"
    fi

    references_root="$skill_dir/references"
    if [ -d "$references_root" ]; then
        reference_files=$(find "$references_root" -type f -print)
        if [ -n "$reference_files" ]; then
            reference_saved_ifs=$IFS
            IFS='
'
            for reference in $reference_files; do
                if [ "$(dirname "$reference")" != "$references_root" ]; then
                    add_error "$reference" "references supports one directory level only"
                fi
                reference_link="references/$(basename "$reference")"
                if ! grep -Fq "$reference_link" "$skill_path"; then
                    add_error "$reference" "not linked directly from the sibling SKILL.md"
                fi
            done
            IFS=$reference_saved_ifs
        fi
    fi

    metadata_path="$skill_dir/agents/openai.yaml"
    if [ ! -f "$metadata_path" ]; then
        add_error "$skill_dir" "missing agents/openai.yaml"
    else
        validate_utf8 "$metadata_path" false
        for field in display_name short_description default_prompt; do
            if ! tr -d '\r' < "$metadata_path" | grep -Eq "^[[:space:]]*$field[[:space:]]*:[[:space:]]*\"[^\"]+\"[[:space:]]*$"; then
                add_error "$metadata_path" "$field must exist and use double quotes"
            fi
        done
        if [ -n "$name" ] && ! grep -Fq "\$$name" "$metadata_path"; then
            add_error "$metadata_path" "default_prompt must reference '\$$name'"
        fi
    fi
}

validate_skill_registry() {
    if [ ! -f "$readme_path" ]; then
        add_error "$agents_root" "missing .agents/README.md"
        return
    fi

    validate_utf8 "$readme_path" false
    registered_skills=$(awk '
        {
            line = $0
            sub(/\r$/, "", line)
            if (!match(line, /^\|[[:space:]]*`[a-z0-9-]+`[[:space:]]*\|/)) next
            name = substr(line, RSTART, RLENGTH)
            sub(/^\|[[:space:]]*`/, "", name)
            sub(/`[[:space:]]*\|$/, "", name)
            if (!seen[name]++) print name
        }
    ' "$readme_path")
    for skill_dir in "$skills_root"/*; do
        [ -d "$skill_dir" ] || continue
        skill_name=$(basename "$skill_dir")
        if ! printf '%s\n' "$registered_skills" | grep -Fxq "$skill_name"; then
            add_error "$readme_path" "skill registry is missing '$skill_name'"
        fi
    done

    if [ -n "$registered_skills" ]; then
        registry_saved_ifs=$IFS
        IFS='
'
        for registered_skill in $registered_skills; do
            if [ ! -d "$skills_root/$registered_skill" ]; then
                add_error "$readme_path" "skill registry contains missing directory '$registered_skill'"
            fi
        done
        IFS=$registry_saved_ifs
    fi
}

markdown_targets() {
    awk '
        {
            line = $0
            while (match(line, /\[[^]]*\]\([^)]+\)/)) {
                link = substr(line, RSTART, RLENGTH)
                open_index = index(link, "(")
                print substr(link, open_index + 1, length(link) - open_index - 1)
                line = substr(line, RSTART + RLENGTH)
            }
        }
    ' "$1"
}

validate_markdown_file() {
    markdown_file=$1
    validate_utf8 "$markdown_file" false
    targets=$(markdown_targets "$markdown_file")
    [ -n "$targets" ] || return

    target_saved_ifs=$IFS
    IFS='
'
    for raw_target in $targets; do
        target=$(printf '%s\n' "$raw_target" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' -e 's/^<*//' -e 's/>*$//')
        [ -n "$target" ] || continue
        if printf '%s\n' "$target" | grep -Eiq '^(https?://|mailto:|#)'; then
            continue
        fi

        target=${target%%#*}
        case "$target" in
            /*|\\*|[A-Za-z]:/*|[A-Za-z]:\\*)
                add_error "$markdown_file" "Markdown link must not use a machine-absolute path: $target"
                continue
                ;;
        esac

        decoded_target=$(decode_uri_path "$target")
        if [ ! -e "$(dirname "$markdown_file")/$decoded_target" ]; then
            add_error "$markdown_file" "local Markdown link does not exist: $target"
        fi
    done
    IFS=$target_saved_ifs
}

for command_name in awk find grep od sed tr; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        printf 'Required system command not found: %s\n' "$command_name" >&2
        exit 2
    fi
done

if [ ! -d "$skills_root" ]; then
    printf 'Skills root not found: %s\n' "$skills_root" >&2
    exit 1
fi

skill_count=0
for skill_dir in "$skills_root"/*; do
    [ -d "$skill_dir" ] || continue
    skill_count=$((skill_count + 1))
done
printf 'Project skills: %s\n' "$skill_count"

for skill_dir in "$skills_root"/*; do
    [ -d "$skill_dir" ] || continue
    validate_skill "$skill_dir"
done
validate_skill_registry

for root_file in AGENTS.md CLAUDE.md; do
    candidate="$repo_root/$root_file"
    if [ -f "$candidate" ]; then
        validate_markdown_file "$candidate"
    fi
done
for markdown_root in "$agents_root" "$repo_root/docs"; do
    [ -d "$markdown_root" ] || continue
    markdown_files=$(find "$markdown_root" -type f -name '*.md' -print)
    [ -n "$markdown_files" ] || continue
    markdown_saved_ifs=$IFS
    IFS='
'
    for markdown_file in $markdown_files; do
        validate_markdown_file "$markdown_file"
    done
    IFS=$markdown_saved_ifs
done

if [ "$errors" -gt 0 ]; then
    printf 'Validation failed: %s error(s), %s warning(s).\n' "$errors" "$warnings"
    exit 1
fi

printf 'Validation passed: 0 errors, %s warning(s).\n' "$warnings"
