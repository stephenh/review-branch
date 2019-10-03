package reviewbranch.apis;

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
      Optional<String> previousRbId,
      Optional<String> bugId) {
    // `git review` stores a single RB-per-branch ID in config; ensure we don't use that
    unsetReviewIdInGitConfig(currentBranch);

    Execute e = git() //
      .arg("review")
      .arg("create")
      .arg("--no-prompt")
      .arg("--parent")
      .arg("HEAD^");
    Optional.ofNullable(args.groups).ifPresent(gs -> e.arg("--groups").arg(gs));
    Optional.ofNullable(args.reviewers).ifPresent(rs -> e.arg("--reviewers").arg(rs));
    Optional.ofNullable(args.testingDone).ifPresent(td -> e.arg("--testing-done").arg(td));
    if (args.publish) {
      e.arg("--publish");
    }
    if (previousRbId.isPresent() || bugId.isPresent()) {
      String rbtFlags = "";
      if (bugId.isPresent()) {
        rbtFlags += " --bugs-closed " + bugId.get();
      }
      if (previousRbId.isPresent()) {
        rbtFlags += " --depends-on=" + previousRbId.get();
      }
      e.arg("--rbt-flags").arg(rbtFlags);
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
    Optional.ofNullable(args.testingDone).ifPresent(td -> e.arg("--testing-done").arg(td));
    Optional.ofNullable(args.diffDescription).ifPresent(dd -> e.arg("--diff-description").arg(dd));
    if (args.publish) {
      e.arg("--publish");
    }
    previousRbId.ifPresent(s -> e.arg("--rbt-flags").arg(" --depends-on=" + s));

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
