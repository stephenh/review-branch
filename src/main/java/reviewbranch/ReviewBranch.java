package reviewbranch;

import java.util.List;
import java.util.Optional;

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

  private static final Logger log = LoggerFactory.getLogger(ReviewBranch.class);
  private final Git git;
  private final ReviewBoard rb;

  public ReviewBranch(Git git, ReviewBoard rb) {
    this.git = git;
    this.rb = rb;
  }

  public void run(Object args) {
    ensureGitNotesConfigured();
    if (args instanceof ReviewArgs) {
      run((ReviewArgs) args);
    } else {
      run((DCommitArgs) args);
    }
  }

  public void ensureGitNotesConfigured() {
    addConfigIfNeeded("notes.displayRef", "ref/notes/reviewid");
    addConfigIfNeeded("notes.displayRef", "ref/notes/reviewlasthash");
    addConfigIfNeeded("notes.rewriteRef", "ref/notes/reviewid");
    addConfigIfNeeded("notes.rewriteRef", "ref/notes/reviewlasthash");
  }
  
  private void addConfigIfNeeded(String key, String value) {
    List<String> current = git.getMultipleValueConfig(key);
    if (!current.contains(value)) {
      git.addMultipleValueConfig(key, value);
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

      Optional<String> rbId = git.getNote("reviewid");
      Optional<String> lastTreeHash = git.getNote("reviewlasthash");
      String currentTreeHash = git.getCurrentTreeHash();

      if (rbId.isPresent()) {
        if (lastTreeHash.isPresent() && lastTreeHash.get().equals(currentTreeHash)) {
          log.info("Skipped RB: " + rbId.get());
        } else {
          rb.updateRbForCurrentCommit(args, rbId.get(), previousRbId);
          log.info("Updated RB: " + rbId.get());
          git.setNote("reviewlasthash", currentTreeHash);
        }
        previousRbId = rbId;
      } else {
        String newRbId = rb.createNewRbForCurrentCommit(args, currentBranch, previousRbId);
        log.info("Created RB: " + newRbId);
        git.setNote("reviewid", newRbId);
        git.setNote("reviewlasthash", currentTreeHash);
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

      Optional<String> rbId = git.getNote("reviewid");
      if (!rbId.isPresent()) {
        throw new IllegalStateException("Cannot dcommit without a previous review");
      }

      rb.dcommit(rbId.get());
      log.info("Updated RB: " + rbId.get());
      previousRbId = rbId;
    }
  }
}
