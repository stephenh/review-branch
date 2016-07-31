package reviewbranch;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.builder.CliBuilder;

/**
 * Creates RBs for a branch, one RB per commit.
 */
public class ReviewBranch {

  @Command(name = "review")
  public static class ReviewArgs {
    @Option(name = { "-r", "--reviewers" }, description = "csv of reviewers (only set on RB creation)")
    public String reviewers;

    @Option(name = { "-g", "--groups" }, description = "csv of groups (only set on RB creation)")
    public String groups;

    @Option(name = { "--publish" }, description = "publish the RB immediately")
    public boolean publish = false;

    @Option(name = { "--testing-done" }, description = "text to add in testing done")
    public String testingDone;
  }

  @Command(name = "dcommit")
  public static class DCommitArgs {
  }

  public static void main(String[] stringArgs) {
    // SingleCommand<ReviewArgs> parser = SingleCommand.singleCommand(ReviewArgs.class);
    // ReviewArgs args = parser.parse(stringArgs);
    // new ReviewBranch(new GitImpl(), new ReviewBoardImpl(), args).run();

    CliBuilder<Object> b = Cli.<Object> builder("review-branch").withDescription("creates lots of RBs");
    b.withCommand(ReviewArgs.class);
    b.withCommand(DCommitArgs.class);
    Object args = b.build().parse(stringArgs);
    new ReviewBranch(new GitImpl(), new ReviewBoardImpl()).run(args);
  }

  private static final Pattern rbIdRegex = Pattern.compile("\\nRB=(\\d+)");
  private static final Logger log = LoggerFactory.getLogger(ReviewBranch.class);
  private final Git git;
  private final ReviewBoard rb;

  public ReviewBranch(Git git, ReviewBoard rb) {
    this.git = git;
    this.rb = rb;
  }

  public void run(Object args) {
    if (args instanceof ReviewArgs) {
      run((ReviewArgs) args);
    } else {
      run((DCommitArgs) args);
    }
  }

  public void run(ReviewArgs args) {
    String currentBranch = git.getCurrentBranch();

    List<String> revs = git.getRevisionsFromOriginMaster();
    log.info("Found revs {}", revs);

    Optional<String> previousRbId = Optional.empty();
    for (String rev : revs) {
      if (!previousRbId.isPresent()) {
        log.info("Resetting to {}", rev);
        git.resetHard(rev);
      } else {
        log.info("Cherry picking {}", rev);
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

  public void run(DCommitArgs args) {
    List<String> revs = git.getRevisionsFromOriginMaster();
    log.info("Found revs {}", revs);

    Optional<String> previousRbId = Optional.empty();
    for (String rev : revs) {
      if (!previousRbId.isPresent()) {
        log.info("Resetting to {}", rev);
        git.resetHard(rev);
      } else {
        log.info("Cherry picking {}", rev);
        git.cherryPick(rev);
      }

      String commitMessage = git.getCurrentCommitMessage();
      Optional<String> rbId = parseRbIdIfAvailable(commitMessage);
      if (!rbId.isPresent()) {
        throw new IllegalStateException("Cannot dcommit without an RB in the commit message");
      }

      rb.dcommit(rbId.get());
      log.info("Updated RB: " + rbId.get());
      previousRbId = rbId;
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
