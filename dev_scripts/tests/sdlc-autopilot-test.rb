require 'minitest/autorun'
require_relative '../sdlc-autopilot'

# Regression tests substitute every subprocess/service method. They do not read
# credentials, contact services, launch Codex/Maven/browsers or change real Git.
class AutopilotRegressionTest < Minitest::Test
  def runner
    instance = SdlcAutopilot.allocate
    instance.instance_variable_set(:@root, '/unused')
    instance.instance_variable_set(:@work, '/unused')
    instance.instance_variable_set(:@issue, 'AIDM-999')
    instance.instance_variable_set(:@state, {})
    instance.define_singleton_method(:phase) { |*| }
    instance.define_singleton_method(:save_state) {}
    instance.define_singleton_method(:sleep) { |*| }
    instance
  end

  def fixture_directory
    Dir.mktmpdir('autopilot-regression-', '/private/tmp')
  end

  def test_normal_feature_paths_are_not_arbitrarily_blocked
    instance = runner
    %w[src/new/file.java docs/duplicate-management.md docs/new-feature.md dev_scripts/new-helper.rb tests/example.test.js].each do |path|
      assert instance.allowed_delivery_path?(path), path
    end
  end

  def test_role_context_keeps_review_in_a_separate_orchestrated_session
    prompt = runner.context
    assert_includes prompt, 'perform ONLY your assigned role'
    assert_includes prompt, 'Do not spawn subagents'
    assert_includes prompt, 'Pending later phases are not blockers'
    assert_includes prompt, 'never bypass or retry denied actions'
  end

  def test_policy_runner_and_generated_artifacts_remain_protected
    instance = runner
    %w[AGENTS.md .agents/skills/test/SKILL.md .codex/config.toml .git/config dev_scripts/sdlc-autopilot.rb dev_scripts/jira-read-issue.sh node_modules/pkg.js target/example.jar tests/test-results/trace.zip ../outside].each do |path|
      refute instance.allowed_delivery_path?(path), path
    end
  end

  def test_codex_roles_keep_sandbox_without_incompatible_flag
    instance = runner
    [true, false].each do |writable|
      args = instance.codex_arguments(writable, 'schema.json', 'result.json')
      assert_equal writable ? 'workspace-write' : 'read-only', args[args.index('--sandbox') + 1]
      assert_includes args, '--json'
      refute_includes args, '--approve-for-me'
      refute_includes args, '--dangerously-bypass-approvals-and-sandbox'
    end
  end

  def test_git_transport_retries_and_uses_https_without_global_config_change
    instance = runner
    calls = []
    instance.define_singleton_method(:command) do |*args, **|
      calls << args
      calls.length < 3 ? ['ssh: connection timed out', false] : ['ok', true]
    end
    assert_equal 'ok', instance.git('fetch', 'origin', 'main')
    assert_equal 3, calls.length
    assert calls.last.include?('url.https://github.com/.insteadOf=git@github.com:')
    refute calls.last.any? { |arg| arg.start_with?('remote.origin.url=') }
    assert calls.last.include?('credential.helper=!gh auth git-credential')
    refute calls.flatten.include?('--global')
  end

  def test_authentication_failure_is_not_hidden_as_a_success
    instance = runner
    instance.define_singleton_method(:command) { |*args, **| ['fatal: authentication failed', false] }
    assert_raises(RuntimeError) { instance.git('push', 'origin', 'branch') }
  end

  def test_changed_pr_head_cannot_merge
    instance = runner
    calls = []
    instance.define_singleton_method(:gh) do |*args, **|
      calls << args
      [JSON.generate('headRefOid' => 'different', 'baseRefName' => 'main', 'isDraft' => false), true]
    end
    assert_raises(RuntimeError) { instance.merge_pr('pr', 'reviewed') }
    refute calls.any? { |args| args.include?('merge') }
  end

  def test_merge_uses_exact_head_without_admin_override
    instance = runner
    calls = []
    instance.define_singleton_method(:gh) do |*args, **|
      calls << args
      if args.include?('merge')
        ['', true]
      elsif args.any? { |arg| arg.start_with?('headRefOid,baseRefName,isDraft') }
        [JSON.generate('headRefOid' => 'reviewed', 'baseRefName' => 'main', 'isDraft' => false), true]
      else
        [JSON.generate('state' => 'MERGED', 'headRefOid' => 'reviewed', 'mergeCommit' => { 'oid' => 'merged' }), true]
      end
    end
    assert_equal 'MERGED', instance.merge_pr('pr', 'reviewed')['state']
    merge = calls.find { |args| args.include?('merge') }
    assert_includes merge, '--match-head-commit'
    refute_includes merge, '--admin'
  end

  def test_main_advance_requires_new_verification
    instance = runner
    instance.define_singleton_method(:gh) do |*args, **|
      if args.any? { |arg| arg.start_with?('headRefOid,baseRefName,isDraft') }
        [JSON.generate('headRefOid' => 'reviewed', 'baseRefName' => 'main', 'isDraft' => false), true]
      elsif args.include?('merge')
        ['behind main', false]
      else
        [JSON.generate('mergeStateStatus' => 'BEHIND', 'headRefOid' => 'reviewed'), true]
      end
    end
    assert_raises(SdlcAutopilot::BaseAdvanced) { instance.merge_pr('pr', 'reviewed') }
  end

  def test_lost_merge_response_recovers_verified_remote_result
    instance = runner
    instance.define_singleton_method(:gh) do |*args, **|
      if args.include?('merge')
        ['connection reset', false]
      elsif args.any? { |arg| arg.start_with?('headRefOid,baseRefName,isDraft') }
        [JSON.generate('headRefOid' => 'reviewed', 'baseRefName' => 'main', 'isDraft' => false), true]
      else
        [JSON.generate('state' => 'MERGED', 'headRefOid' => 'reviewed', 'mergeCommit' => { 'oid' => 'merged' }), true]
      end
    end
    assert_equal 'merged', instance.merge_pr('pr', 'reviewed').dig('mergeCommit', 'oid')
  end

  def test_interrupt_reaps_only_owned_command_group
    instance = runner
    instance.instance_variable_set(:@run, fixture_directory)
    instance.instance_variable_set(:@serial, 0)
    killed = []
    reaped = []
    Process.stub(:spawn, 12345) do
      Process.stub(:wait2, ->(*) { raise Interrupt }) do
        Process.stub(:kill, ->(*args) { killed << args }) do
          Process.stub(:wait, ->(pid) { reaped << pid }) do
            assert_raises(Interrupt) { instance.command('mock-command') }
          end
        end
      end
    end
    assert_equal [['TERM', -12345], ['KILL', -12345]], killed
    assert_equal [12345], reaped
  end

  def checks_runner(watch_success, buckets)
    instance = runner
    instance.define_singleton_method(:gh) do |*args, **|
      args.include?('--watch') ? ['', watch_success] : [JSON.generate(buckets), true]
    end
    instance
  end

  def test_failed_ci_is_never_accepted
    instance = checks_runner(false, [{ 'name' => 'Build and test (Java 17)', 'bucket' => 'pending' }])
    refute instance.await_checks('pr')
  end

  def test_required_pass_with_another_failure_is_rejected
    instance = checks_runner(true, [{ 'name' => 'Build and test (Java 17)', 'bucket' => 'pass' }, { 'name' => 'other', 'bucket' => 'fail' }])
    assert_raises(RuntimeError) { instance.await_checks('pr') }
  end

  def test_all_passing_checks_are_accepted
    instance = checks_runner(true, [{ 'name' => 'Build and test (Java 17)', 'bucket' => 'pass' }])
    assert instance.await_checks('pr')
  end

  def test_extra_source_after_review_is_rejected
    instance = runner
    instance.instance_variable_set(:@state, { 'tested_commit' => 'candidate', 'reviewed_commit' => 'candidate' })
    instance.define_singleton_method(:git) { |*| 'src/file.java' }
    instance.define_singleton_method(:changed_paths) { [] }
    assert_raises(RuntimeError) { instance.validate_publication_head }
  end

  def test_only_selected_issues_review_record_is_allowed
    instance = runner
    instance.instance_variable_set(:@state, { 'tested_commit' => 'candidate', 'reviewed_commit' => 'candidate' })
    instance.define_singleton_method(:git) { |*| 'docs/sdlc/reviews/AIDM-999.md' }
    instance.define_singleton_method(:changed_paths) { [] }
    instance.validate_publication_head
  end

  def test_pr_creation_recovers_a_lost_response_without_duplicates
    instance = runner
    lists = 0
    creates = 0
    instance.define_singleton_method(:gh) do |*args, **|
      lists += 1
      [JSON.generate(lists == 1 ? [] : [{ 'url' => 'https://github.com/davehen/registry-office/pull/99' }]), true]
    end
    instance.define_singleton_method(:command) do |*args, **|
      creates += 1
      ['connection reset', false]
    end
    assert_equal 'https://github.com/davehen/registry-office/pull/99', instance.find_or_create_pr('branch', 'title', 'body')
    assert_equal 1, creates
  end

  def test_resume_identity_and_browser_paths_are_per_issue
    instance = runner
    context = instance.context
    assert_includes context, 'AIDM-999'
    refute_includes context, 'Deliver only AIDM-184'
  end

  def test_browser_reports_follow_forced_output_and_real_suite_is_run
    directory = fixture_directory
    instance = runner
    instance.instance_variable_set(:@run, directory)
    instance.instance_variable_set(:@work, directory)
    FileUtils.mkdir_p(File.join(directory, 'dev_scripts/ui_tests'))
    %w[playwright.config.cjs playwright.real.config.cjs].each { |config| File.write(File.join(directory, 'dev_scripts/ui_tests', config), '') }
    FileUtils.mkdir_p(File.join(directory, 'target/surefire-reports'))
    File.write(File.join(directory, 'target/surefire-reports/TEST-test.xml'), '<testsuite tests="1" failures="0" errors="0" skipped="0"/>')
    commands = []
    stopped = []
    instance.define_singleton_method(:changed_paths) { [] }
    instance.define_singleton_method(:git) { |*| 'candidate' }
    instance.define_singleton_method(:start_owned_app) { |*| { 'pid' => 123, 'url' => 'http://127.0.0.1:8090' } }
    instance.define_singleton_method(:stop_owned_app) { |app| stopped << app }
    instance.define_singleton_method(:command) do |*args, **options|
      commands << [args, options]
      if args.include?('--output')
        output = args[args.index('--output') + 1]
        FileUtils.mkdir_p(output)
        File.write(File.join(output, '.last-run.json'), JSON.generate('status' => 'passed', 'failedTests' => []))
      end
      ['', true]
    end
    assert instance.local_tests
    browser_calls = commands.select { |args, _| args.include?('--output') }
    assert_equal 2, browser_calls.length
    assert browser_calls.all? { |_, options| options[:env]['REGISTRY_BASE_URL'] == 'http://127.0.0.1:8090' }
    assert_equal 1, stopped.length
    assert_equal 2, instance.instance_variable_get(:@state)['browser_reports'].length
  end
end
