#!/usr/bin/env bash
set -euo pipefail

# Read-only Jira intake bridge for the AI-assisted SDLC pilot.
# Credentials remain in the macOS Keychain; this script never writes to Jira
# or stores issue content in the repository.

readonly JIRA_SITE="https://arhs-group.atlassian.net"
readonly KEYCHAIN_SERVICE="registry-office-sdlc-jira"
readonly PILOT_EPIC="AIDM-181"
readonly FIELDS="summary,description,labels,priority,components,attachment,issuelinks,parent,status,issuetype"

if [[ $# -ne 1 ]]; then
  echo "Usage: $(basename "$0") AIDM-123" >&2
  exit 1
fi

ISSUE_KEY="$1"
if [[ ! "$ISSUE_KEY" =~ ^AIDM-[0-9]+$ ]]; then
  echo "Error: only AIDM work items are in scope for this pilot." >&2
  exit 1
fi

for command in curl ruby security; do
  command -v "$command" >/dev/null || {
    echo "Error: required command '$command' is unavailable." >&2
    exit 1
  }
done

JIRA_EMAIL="$(security find-generic-password -a "jira-email" -s "$KEYCHAIN_SERVICE" -w)"
JIRA_TOKEN="$(security find-generic-password -a "jira-api-token" -s "$KEYCHAIN_SERVICE" -w)"
trap 'unset JIRA_EMAIL JIRA_TOKEN' EXIT

curl --fail --silent --show-error \
  --user "$JIRA_EMAIL:$JIRA_TOKEN" \
  --get \
  --data-urlencode "fields=$FIELDS" \
  "$JIRA_SITE/rest/api/3/issue/$ISSUE_KEY" |
  ruby -rjson -e '
    issue = JSON.parse(STDIN.read)
    expected_epic = ARGV.fetch(0)
    actual_epic = issue.dig("fields", "parent", "key")

    unless actual_epic == expected_epic
      warn "Error: #{issue.fetch("key")} is not a child of #{expected_epic}."
      exit 1
    end

    puts JSON.pretty_generate(issue)
  ' "$PILOT_EPIC"
