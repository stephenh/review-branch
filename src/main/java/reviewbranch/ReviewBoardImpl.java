package reviewbranch;

import static org.apache.commons.lang3.StringUtils.chomp;

import joist.util.Execute;
import joist.util.Execute.BufferedResult;
import reviewbranch.ReviewBranch.ReviewBranchArgs;

/**
 * Implements {@link ReviewBoard} but via calling the {@code git review} commands.
 */
public class ReviewBoardImpl implements ReviewBoard {

  @Override
  public String createNewRbForCurrentCommit(ReviewBranchArgs args, String currentBranch) {
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

    BufferedResult r = e.toBuffer();
    failIfInvalidResult(r);

    // we can leave the reviewid set because our next invocation will unset it
    return getReviewIdInGitConfig(currentBranch);
  }

  @Override
  public void updateRbForCurrentCommit(ReviewBranchArgs args, String rbId) {
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