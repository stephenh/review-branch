package reviewbranch;

import static org.apache.commons.lang3.StringUtils.chomp;

import java.util.Optional;

import joist.util.Execute;
import joist.util.Execute.BufferedResult;
import reviewbranch.commands.ReviewCommand;

/**
 * Implements {@link ReviewBoard} but via calling the {@code git review} commands.
 */
public class ReviewBoardImpl implements ReviewBoard {

  @Override
  public String createNewRbForCurrentCommit(//
    ReviewCommand args,
    String currentBranch,
    Optional<String> previousRbId) {
    // `git review` stores a single RB-per-branch ID in config; ensure we don't use that
    unsetReviewIdInGitConfig(currentBranch);

    Execute e = git() //
      .arg("review")
      .arg("create")
      .arg("--no-prompt")
      .arg("--parent")
      .arg("HEAD^");
    if (args.groups != null) {
      e.arg("--groups").arg(args.groups);
    }
    if (args.reviewers != null) {
      e.arg("--reviewers").arg(args.reviewers);
    }
    if (args.testingDone != null) {
      e.arg("--testing-done").arg(args.testingDone);
    }
    if (args.publish) {
      e.arg("--publish");
    }
    if (previousRbId.isPresent()) {
      e.arg("--rbt-flags").arg(" --depends-on=" + previousRbId.get());
    }

    BufferedResult r = e.toBuffer();
    failIfInvalidResult(r);

    // we can leave the reviewid set because our next invocation will unset it
    return getReviewIdInGitConfig(currentBranch);
  }

  @Override
  public void updateRbForCurrentCommit(ReviewCommand args, String rbId, Optional<String> previousRbId) {
    Execute e = git() //
      .arg("review")
      .arg("update")
      .arg("-r")
      .arg(rbId)
      .arg("--parent")
      .arg("HEAD^");
    if (args.testingDone != null) {
      e.arg("--testing-done").arg(args.testingDone);
    }
    if (args.publish) {
      e.arg("--publish");
    }
    if (previousRbId.isPresent()) {
      e.arg("--rbt-flags").arg(" --depends-on=" + previousRbId.get());
    }

    BufferedResult r = e.toBuffer();
    failIfInvalidResult(r);
  }

  @Override
  public void dcommit(String rbId) {
    Execute e = git().arg("review").arg("dcommit").arg("-r").arg(rbId);
    BufferedResult r = e.toBuffer();
    failIfInvalidResult(r);
  }

  private static Execute git() {
    return new Execute("git").addEnvPaths();
  }

  private static void failIfInvalidResult(BufferedResult r) {
    if (r.exitValue != 0) {
      System.out.println(r.out);
      System.err.println(r.err);
      throw new IllegalStateException("git failed: " + r.exitValue);
    }
  }

  private String getReviewIdInGitConfig(String currentBranch) {
    BufferedResult r = git().arg("config").arg("branch." + currentBranch + ".reviewid").toBuffer();
    failIfInvalidResult(r);
    return chomp(r.out);
  }

  private void unsetReviewIdInGitConfig(String currentBranch) {
    BufferedResult r = git().arg("config").arg("--unset").arg("branch." + currentBranch + ".reviewid").toBuffer();
    if (r.exitValue == 5) {
      // just means the unset didn't work, which is okay
      return;
    }
    failIfInvalidResult(r);
  }

}
