package reviewbranch.commands;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import reviewbranch.apis.Git;
import reviewbranch.apis.ReviewBoard;

@Command(name = "review", description = "Creates/updates an RB for each new/updated commit in your branch")
public class ReviewCommand extends AbstractCommand {

  private static final Logger log = LoggerFactory.getLogger(ReviewCommand.class);
  private static final Pattern indexRegex = Pattern.compile("\nindex \\w+\\.\\.\\w+ \\d+\n");
  private static final Pattern rbRegex = Pattern.compile("RB=(\\d+)");
  private static final Pattern bugRegex = Pattern.compile("BUG=([\\w\\d-]+)");

  @Option(name = { "-r", "--reviewers" }, description = "csv of reviewers (only set on RB creation)")
  public String reviewers;

  @Option(name = { "-g", "--groups" }, description = "csv of groups (only set on RB creation)")
  public String groups;

  @Option(name = { "--publish" }, description = "publish the RB immediately")
  public boolean publish = false;

  @Option(name = { "--testing-done" }, description = "text to add in testing done")
  public String testingDone;

  @Override
  public void run(Git git, ReviewBoard rb) {
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
      String message = git.getCurrentCommitMessage();

      // See if this is a rebased commit with an existing reviewid
      if (!rbId.isPresent()) {
        if (message != null) {
          Matcher m = rbRegex.matcher(message);
          if (m.find()) {
            rbId = Optional.of(m.group(1));
          }
        }
      }

      if (rbId.isPresent()) {
        if (lastDiffHash.isPresent() && lastDiffHash.get().equals(currentDiffHash)) {
          log.info("Skipped RB: " + rbId.get());
        } else {
          if (rbId.get().contains("\n")) {
            // this is a squashed/fixed commit
            rbId = rbId.map(id -> StringUtils.substringBefore(id, "\n"));
          }
          rb.updateRbForCurrentCommit(this, rbId.get(), previousRbId);
          log.info("Updated RB: " + rbId.get());
          git.setNote("reviewid", rbId.get());
          git.setNote("reviewlasthash", currentDiffHash);
        }
        previousRbId = rbId;
      } else {
        Optional<String> bugId = findBugIdInCommitMessage(message);
        String newRbId = rb.createNewRbForCurrentCommit(this, currentBranch, previousRbId, bugId);
        log.info("Created RB: " + newRbId);
        git.setNote("reviewid", newRbId);
        git.setNote("reviewlasthash", currentDiffHash);
        previousRbId = Optional.of(newRbId);
      }
    }
  }

  private static Optional<String> findBugIdInCommitMessage(String message) {
    if (message != null) {
      Matcher m = bugRegex.matcher(message);
      if (m.find()) {
        return Optional.of(m.group(1));
      }
    }
    return Optional.empty();
  }

  private static String stripIndexAndHash(String diff) {
    // the index line includes hashes that will change after rebases
    diff = indexRegex.matcher(diff).replaceAll("\n");
    return Hashing.sha1().hashString(diff, Charsets.UTF_8).toString();
  }
}
