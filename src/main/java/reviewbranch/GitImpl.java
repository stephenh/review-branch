package reviewbranch;

import static org.apache.commons.lang3.StringUtils.chomp;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Seq;

import joist.util.Execute;
import joist.util.Execute.BufferedResult;

public class GitImpl implements Git {

  @Override
  public String getCurrentBranch() {
    BufferedResult r = git().arg("rev-parse").arg("--abbrev-ref").arg("HEAD").toBuffer();
    failIfInvalidResult(r);
    return chomp(r.out);
  }

  @Override
  public List<String> getRevisionsFromOriginMaster() {
    BufferedResult r = git().arg("rev-list").arg("--abbrev-commit").arg("--reverse").arg("origin/master..HEAD").toBuffer();
    failIfInvalidResult(r);
    return Seq.of(chomp(r.out).split("\n")).toList();
  }

  @Override
  public void checkout(String revision) {
    BufferedResult r = git().arg("checkout").arg(revision).toBuffer();
    failIfInvalidResult(r);
  }

  @Override
  public void resetHard(String revision) {
    BufferedResult r = git().arg("reset").arg("--hard").arg(revision).toBuffer();
    failIfInvalidResult(r);
  }

  @Override
  public void cherryPick(String revision) {
    // To ensure we don't change the hash (which isn't a huge deal, but is still nice), get the current committer/author information
    BufferedResult r1 = git().arg("log").arg("--format='%cd!%ce!%ad!%ae'").arg(revision + "^.." + revision).toBuffer();
    failIfInvalidResult(r1);
    String[] info = chomp(r1.out).split("!");
    String committerDate = info[0];
    String committerEmail = info[1];

    BufferedResult r2 = git()
      .env("GIT_COMMITTER_EMAIL", committerEmail)
      .env("GIT_COMMITTER_DATE", committerDate)
      .arg("cherry-pick")
      .arg(revision)
      .toBuffer();
    failIfInvalidResult(r2);
  }

  @Override
  public String getCurrentCommitMessage() {
    BufferedResult r = git().arg("log").arg("-1").arg("--pretty=%B").toBuffer();
    failIfInvalidResult(r);
    return StringUtils.stripEnd(r.out, "\n");
  }

  @Override
  public void amendCurrentCommitMessage(String newMessage) {
    BufferedResult r = git().arg("commit").arg("--amend").arg("-o").arg("-m").arg(newMessage).toBuffer();
    failIfInvalidResult(r);
  }

  private static void failIfInvalidResult(BufferedResult r) {
    if (r.exitValue != 0) {
      System.out.println(r.out);
      System.err.println(r.err);
      throw new IllegalStateException("git failed");
    }
  }

  private static Execute git() {
    return new Execute("git").addEnvPaths();
  }

}
