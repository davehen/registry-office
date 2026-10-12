#!/usr/bin/env ruby
# Run in the user's terminal: ruby dev_scripts/jira-sync-snapshot.rb AIDM-182
# Publishes Jira data, never credentials, outside Git for Codex to read.
# Readers must require status=ok and fetched_at no older than 180 seconds.
require 'json'
require 'open3'
require 'time'
require 'tempfile'

issue_key = ARGV.first
abort 'Usage: ruby dev_scripts/jira-sync-snapshot.rb AIDM-123' unless
  ARGV.length == 1 && /\AAIDM-[0-9]+\z/.match?(issue_key)

directory = "/private/tmp/registry-office-jira-poc-#{Process.uid}"
begin
  Dir.mkdir(directory, 0700)
rescue Errno::EEXIST
  # Reuse only a private directory owned by the current user.
end
stat = File.lstat(directory)
abort 'Unsafe snapshot directory; refusing to use it.' unless
  stat.directory? && !stat.symlink? && stat.uid == Process.uid && (stat.mode & 0777) == 0700

# Exclusive creation prevents two synchronizers from overwriting each other.
lock_path = File.join(directory, "#{issue_key}.lock")
lock = File.open(lock_path, File::WRONLY | File::CREAT | File::EXCL, 0600)
snapshot_path = File.join(directory, "#{issue_key}.json")
at_exit do
  File.unlink(snapshot_path) if File.exist?(snapshot_path)
  lock.close
  File.unlink(lock_path)
end
Signal.trap('INT') { exit }
Signal.trap('TERM') { exit }
$stdout.sync = true
puts "Snapshot: #{snapshot_path}"
puts 'Refreshing every 60 seconds. Leave this terminal running; Ctrl+C stops and removes the snapshot.'

bridge = File.expand_path('jira-read-issue.sh', __dir__)
loop do
  # Credentials are obtained by the approved bridge in the user's process.
  # Neither raw Jira content nor command error output is printed to the console.
  raw, _error, result = Open3.capture3('bash', bridge, issue_key)
  envelope = {
    'source' => 'https://arhs-group.atlassian.net',
    'issue_key' => issue_key,
    'attempted_at' => Time.now.utc.iso8601,
    'status' => 'error'
  }
  if result.success?
    begin
      issue = JSON.parse(raw)
      raise 'Unexpected Jira issue' unless issue['key'] == issue_key &&
        issue.dig('fields', 'parent', 'key') == 'AIDM-181'
      envelope.merge!('status' => 'ok', 'fetched_at' => Time.now.utc.iso8601, 'issue' => issue)
    rescue JSON::ParserError, RuntimeError
      envelope['error'] = 'Jira returned invalid or out-of-scope data.'
    end
  else
    envelope['error'] = 'Jira read failed. Check connectivity and the approved Keychain credentials.'
  end

  # Replace the whole snapshot atomically, including on failure, so readers
  # cannot mistake a previous successful response for current requirements.
  Tempfile.create([issue_key, '.json'], directory) do |temporary|
    temporary.chmod(0600)
    temporary.write(JSON.pretty_generate(envelope))
    temporary.flush
    File.rename(temporary.path, snapshot_path)
  end
  puts "#{envelope['attempted_at']} #{issue_key}: #{envelope['status']}"
  sleep 60
end
