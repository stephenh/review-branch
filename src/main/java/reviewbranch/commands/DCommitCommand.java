package reviewbranch.commands;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rvesse.airline.annotations.Command;

import reviewbranch.apis.Git;
import reviewbranch.apis.ReviewBoard;

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

}
