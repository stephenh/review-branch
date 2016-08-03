package reviewbranch;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.builder.CliBuilder;
import com.github.rvesse.airline.help.Help;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

/**
 * Creates RBs for a branch, one RB per commit.
 */
public class ReviewBranch {

  private static final Pattern indexRegex = Pattern.compile("\nindex \\w+\\.\\.\\w+ \\d+\n");

  @Command(name = "review", description = "Creates/updates an RB for each new/updated commit in your branch")
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

  @Command(name = "dcommit", description = "Stamps each commit in your branch with its RB's approval information")
  public static class DCommitArgs {
  }

  public static void main(String[] stringArgs) {
    CliBuilder<Object> b = Cli.<Object> builder("review-branch").withDescription("creates lots of RBs");
    b.withCommand(ReviewArgs.class);
    b.withCommand(DCommitArgs.class);
    b.withDefaultCommand(Help.class);
    Cli<Object> cli = b.build();
    Object args = cli.parse(stringArgs);
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
    if (args instanceof ReviewArgs) {
      ensureGitNotesConfigured();
      run((ReviewArgs) args);
    } else if (args instanceof DCommitArgs) {
      ensureGitNotesConfigured();
      run((DCommitArgs) args);
    } else if (args instanceof Runnable) {
      ((Runnable) args).run();
    }
  }

  public void ensureGitNotesConfigured() {
    addConfigIfNeeded("notes.displayRef", "refs/notes/reviewid");
    addConfigIfNeeded("notes.displayRef", "refs/notes/reviewlasthash");
    addConfigIfNeeded("notes.rewriteRef", "refs/notes/reviewid");
    addConfigIfNeeded("notes.rewriteRef", "refs/notes/reviewlasthash");
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
      // We don't amend commits, so a straight reset each time is fine
      log.info("Resetting to {}", rev);
      git.resetHard(rev);

      Optional<String> rbId = git.getNote("reviewid");
      Optional<String> lastDiffHash = git.getNote("reviewlasthash");
      String currentDiffHash = stripIndexAndHash(git.getCurrentDiff());

      if (rbId.isPresent()) {
        if (lastDiffHash.isPresent() && lastDiffHash.get().equals(currentDiffHash)) {
          log.info("Skipped RB: " + rbId.get());
        } else {
          if (rbId.get().contains("\n")) {
            // this is a squased/fixed commit
            rbId = rbId.map(id -> StringUtils.substringBefore(id, "\n"));
            rb.updateRbForCurrentCommit(args, rbId.get(), previousRbId);
            log.info("Updated RB: " + rbId.get());
            git.setNote("reviewid", rbId.get());
            git.setNote("reviewlasthash", currentDiffHash);
          } else {
            rb.updateRbForCurrentCommit(args, rbId.get(), previousRbId);
            log.info("Updated RB: " + rbId.get());
            git.setNote("reviewlasthash", currentDiffHash);
          }
        }
        previousRbId = rbId;
      } else {
        String newRbId = rb.createNewRbForCurrentCommit(args, currentBranch, previousRbId);
        log.info("Created RB: " + newRbId);
        git.setNote("reviewid", newRbId);
        git.setNote("reviewlasthash", currentDiffHash);
        previousRbId = Optional.of(newRbId);
      }
    }
  }

  public void run(DCommitArgs args) {
    List<String> revs = git.getRevisionsFromOriginMaster();
    log.info("Found revs {}", revs);

    boolean firstRev = true;

    for (String rev : revs) {
      if (firstRev) {
        log.info("Resetting to {}", rev);
        git.resetHard(rev);
        firstRev = false;
      } else {
        String lastAmendedCommit = git.getCurrentCommit();
        // before we cherry pick, go get our reviewid
        git.resetHard(rev);
        Optional<String> rbId = git.getNote("reviewid");
        Optional<String> reviewlasthash = git.getNote("reviewlasthash");
        if (!rbId.isPresent()) {
          throw new IllegalStateException("Cannot dcommit without a previous review");
        }
        // now we can go back and cherry pick
        git.resetHard(lastAmendedCommit);
        log.info("Cherry picking {}", rev);
        git.cherryPick(rev);
        // restore the metadata on the picked commit
        git.setNote("reviewid", rbId.get());
        git.setNote("reviewlasthash", reviewlasthash.get());
      }

      Optional<String> rbId = git.getNote("reviewid");
      if (!rbId.isPresent()) {
        throw new IllegalStateException("Cannot dcommit without a previous review");
      }

      rb.dcommit(rbId.get());
      log.info("Updated RB: " + rbId.get());
    }
  }

  private static String stripIndexAndHash(String diff) {
    // the index line includes hashes that will change after rebases
    diff = indexRegex.matcher(diff).replaceAll("\n");
    return Hashing.sha1().hashString(diff, Charsets.UTF_8).toString();
  }
}
