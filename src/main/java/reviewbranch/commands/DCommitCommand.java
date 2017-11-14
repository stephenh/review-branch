package reviewbranch.commands;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rvesse.airline.annotations.Command;

import reviewbranch.apis.Git;
import reviewbranch.apis.ReviewBoard;
import reviewbranch.apis.ReviewId;

@Command(name = "dcommit", description = "Stamps each commit in your branch with its RB's approval information")
public class DCommitCommand extends AbstractCommand {

  private static final Logger log = LoggerFactory.getLogger(DCommitCommand.class);

  @Override
  public void run(Git git, ReviewBoard rb) {
    List<String> revs = git.getRevisionsFromOriginMaster();
    log.info("Found revs {}", revs);

    boolean firstRev = true;
    for (String rev : revs) {
      if (firstRev) {
        log.info("Resetting to {}", rev);
        git.resetHard(rev);
        firstRev = false;
      } else {
        // normally we would just cherry pick rev, but that
        // looses the notes, so we need to resetHard to rev,
        // copy it's notes, then jump back to where we were
        String lastAmendedCommit = git.getCurrentCommit();
        git.resetHard(rev);
        String message = git.getCurrentCommitMessage();
        Optional<String> rbId = ReviewId.getFromNoteOrCommitMessage(git, message);
        Optional<String> reviewlasthash = git.getNote("reviewlasthash");
        git.resetHard(lastAmendedCommit);

        // now we can go back and cherry pick
        log.info("Cherry picking {}", rev);
        git.cherryPick(rev);

        // and restore the metadata on the picked commit (although
        // if we didn't have any, e.g. this is a new un-RB'd commit,
        // just skip it and keep going)
        if (rbId.isPresent()) {
          git.setNote("reviewid", rbId.get());
          git.setNote("reviewlasthash", reviewlasthash.get());
        }
      }

      Optional<String> rbId = git.getNote("reviewid");
      if (rbId.isPresent()) {
        rb.dcommit(rbId.get());
        log.info("Updated RB: " + rbId.get());
      } else {
        log.info("Skipped rev: {} (no RB found)", rev);
      }
    }
  }

}
