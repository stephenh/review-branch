package reviewbranch;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rvesse.airline.SingleCommand;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;

/**
 * Creates RBs for a branch, one RB per commit.
 */
public class ReviewBranch {

  @Command(name = "review-branch")
  public static class ReviewBranchArgs {
    @Option(name = { "-r", "--reviewers" }, description = "csv of reviewers (only set on RB creation)")
    public String reviewers;

    @Option(name = { "-g", "--groups" }, description = "csv of groups (only set on RB creation)")
    public String groups;

    @Option(name = { "--publish" }, description = "publish the RB immediately")
    public boolean publish = false;

    @Option(name = { "--testing-done" }, description = "text to add in testing done")
    public String testingDone;
  }

  public static void main(String[] stringArgs) {
    SingleCommand<ReviewBranchArgs> parser = SingleCommand.singleCommand(ReviewBranchArgs.class);
    ReviewBranchArgs args = parser.parse(stringArgs);
    new ReviewBranch(new GitImpl(), new ReviewBoardImpl(), args).run();
  }

  private static final Pattern rbIdRegex = Pattern.compile("\\nRB=(\\d+)");
  private static final Logger log = LoggerFactory.getLogger(ReviewBranch.class);
  private final Git git;
  private final ReviewBoard rb;
  private final ReviewBranchArgs args;

  public ReviewBranch(Git git, ReviewBoard rb, ReviewBranchArgs args) {
    this.git = git;
    this.rb = rb;
    this.args = args;
  }

  public void run() {
    String currentBranch = git.getCurrentBranch();

    List<String> revs = git.getRevisionsFromOriginMaster();
    log.info("Found revs {}", revs);

    Optional<String> previousRbId = Optional.empty();

    for (String rev : revs) {
      log.info("Checking out {}", rev);

      if (!previousRbId.isPresent()) {
        git.resetHard(rev);
      } else {
        git.cherryPick(rev);
      }

      String commitMessage = git.getCurrentCommitMessage();
      Optional<String> rbId = parseRbIdIfAvailable(commitMessage);
      if (rbId.isPresent()) {
        rb.updateRbForCurrentCommit(args, rbId.get(), previousRbId);
        log.info("Updated RB: " + rbId.get());
        previousRbId = rbId;
      } else {
        String newRbId = rb.createNewRbForCurrentCommit(args, currentBranch, previousRbId);
        log.info("Created RB: " + newRbId);
        git.amendCurrentCommitMessage(commitMessage + "\n\nRB=" + newRbId);
        previousRbId = Optional.of(newRbId);
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
