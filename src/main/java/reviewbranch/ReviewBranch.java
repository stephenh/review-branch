package reviewbranch;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates RBs for a branch, one RB per commit.
 */
public class ReviewBranch {

  public static void main(String[] args) {
    ReviewBranch r = new ReviewBranch(new GitImpl(), new ReviewBoardImpl());
    r.run();
  }

  private static final Pattern rbIdRegex = Pattern.compile("\\nRB=(\\d+)");
  private static final Logger log = LoggerFactory.getLogger(ReviewBranch.class);
  private final Git git;
  private final ReviewBoard rb;

  public ReviewBranch(Git git, ReviewBoard rb) {
    this.git = git;
    this.rb = rb;
  }

  public void run() {
    String currentBranch = git.getCurrentBranch();

    List<String> revs = git.getRevisionsFromOriginMaster();
    log.info("Found revs {}", revs);

    boolean firstCommit = true;

    for (String rev : revs) {
      log.info("Checking out {}", rev);

      if (firstCommit) {
        git.resetHard(rev);
        firstCommit = false;
      } else {
        git.cherryPick(rev);
      }

      String commitMessage = git.getCurrentCommitMessage();
      Optional<String> rbId = parseRbIdIfAvailable(commitMessage);

      if (rbId.isPresent()) {
        rb.updateRbForCurrentCommit(rbId.get());
        log.info("Updated RB: " + rbId.get());
      } else {
        String newRbId = rb.createNewRbForCurrentCommit(currentBranch);
        log.info("Created RB: " + newRbId);
        git.amendCurrentCommitMessage(commitMessage + "\n\nRB=" + newRbId);
      }
    }
  }

  private static final Optional<String> parseRbIdIfAvailable(String commitMessage) {
    Matcher m = rbIdRegex.matcher(commitMessage);
    if (m.find()) {
      return Optional.of(m.group(1));
    } else {
      return Optional.empty();
    }
  }
}
