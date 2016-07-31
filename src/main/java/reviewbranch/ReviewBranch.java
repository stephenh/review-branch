package reviewbranch;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates RBs for a branch, one RB per commit.
 */
public class ReviewBranch {

  public static void main(String[] args) {
  }

  private static final Logger log = LoggerFactory.getLogger(ReviewBranch.class);
  private final Git git;
  private final ReviewBoard rb;

  public ReviewBranch(Git git, ReviewBoard rb) {
    this.git = git;
    this.rb = rb;
  }

  public void run() {
    List<String> revs = git.getRevisionsFromOriginMaster();
    log.info("Found revs {}", revs);
    for (String rev : revs) {
      log.info("Checking out {}", rev);
      git.checkout(rev);

      String rbId = rb.createNewRbForCurrentCommit();
      git.amendCurrentCommitMessage("RB=" + rbId);
    }
  }
}
