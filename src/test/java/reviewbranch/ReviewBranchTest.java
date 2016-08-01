package reviewbranch;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.jooq.lambda.Seq;
import org.junit.After;
import org.junit.Test;

import reviewbranch.ReviewBranch.DCommitArgs;
import reviewbranch.ReviewBranch.ReviewArgs;

@SuppressWarnings({ "unchecked" })
public class ReviewBranchTest {

  private final Git git = mock(Git.class);
  private final ReviewBoard rb = mock(ReviewBoard.class);
  private final ReviewArgs args = new ReviewArgs();
  private final ReviewBranch b = new ReviewBranch(git, rb);

  @After
  public void after() {
    verifyNoMoreInteractions(git, rb);
  }

  @Test
  public void createNewRbForOneCommit() {
    // given we want to review one new commit
    when(git.getCurrentBranch()).thenReturn("branch1");
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA").toList());
    when(git.getNote("reviewid")).thenReturn(Optional.empty());
    when(git.getNote("reviewlasthash")).thenReturn(Optional.empty());
    when(git.getCurrentTreeHash()).thenReturn("commitAtree");
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.empty())).thenReturn("1");
    // when ran
    b.run(args);
    // then we post a new RB for the current commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).getNote("reviewid");
    verify(git).getNote("reviewlasthash");
    verify(git).getCurrentTreeHash();
    verify(rb).createNewRbForCurrentCommit(args, "branch1", Optional.empty());
    verify(git).setNote("reviewid", "1");
    verify(git).setNote("reviewlasthash", "commitAtree");
  }

  @Test
  public void updateRbForOneCommit() {
    // given we want to update one commit
    when(git.getCurrentBranch()).thenReturn("branch1");
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA").toList());
    when(git.getNote("reviewid")).thenReturn(Optional.of("1"));
    when(git.getNote("reviewlasthash")).thenReturn(Optional.of("tree1"));
    when(git.getCurrentTreeHash()).thenReturn("tree2");
    // when ran
    b.run(args);
    // then we post an update to RB for the current commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).getNote("reviewid");
    verify(git).getNote("reviewlasthash");
    verify(git).getCurrentTreeHash();
    verify(rb).updateRbForCurrentCommit(args, "1", Optional.empty());
    verify(git).setNote("reviewlasthash", "tree2");
  }

  @Test
  public void createNewRbForTwoCommits() {
    // given we want to review two new commits
    when(git.getCurrentBranch()).thenReturn("branch1");
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA", "commitB").toList());
    // commitA
    when(git.getNote("reviewid")).thenReturn(Optional.empty(), Optional.empty());
    when(git.getNote("reviewlasthash")).thenReturn(Optional.empty(), Optional.empty());
    when(git.getCurrentTreeHash()).thenReturn("tree1", "tree2");
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.empty())).thenReturn("1");
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.of("1"))).thenReturn("2");
    // when ran
    b.run(args);
    // then we post a new RB for the current commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).cherryPick("commitB");
    verify(git, atLeast(2)).getNote("reviewid");
    verify(git, atLeast(2)).getNote("reviewlasthash");
    verify(git, atLeast(2)).getCurrentTreeHash();
    verify(rb).createNewRbForCurrentCommit(args, "branch1", Optional.empty());
    verify(rb).createNewRbForCurrentCommit(args, "branch1", Optional.of("1"));
    // commitA
    verify(git).setNote("reviewid", "1");
    verify(git).setNote("reviewlasthash", "tree1");
    // commitB
    verify(git).setNote("reviewid", "2");
    verify(git).setNote("reviewlasthash", "tree2");
  }

  @Test
  public void skipFirstRbIfItsUnchanged() {
    // given we want to review two commits
    when(git.getCurrentBranch()).thenReturn("branch1");
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA", "commitB").toList());
    // and the first one already has an id and unchanged tree hash
    when(git.getNote("reviewid")).thenReturn(Optional.of("1"), Optional.empty());
    when(git.getNote("reviewlasthash")).thenReturn(Optional.of("tree1"), Optional.empty());
    when(git.getCurrentTreeHash()).thenReturn("tree1", "tree2");
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.empty())).thenReturn("1");
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.of("1"))).thenReturn("2");
    // when ran
    b.run(args);
    // then we post a new RB for the 2nd commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).cherryPick("commitB");
    verify(git, atLeast(2)).getNote("reviewid");
    verify(git, atLeast(2)).getNote("reviewlasthash");
    verify(git, atLeast(2)).getCurrentTreeHash();
    verify(rb).createNewRbForCurrentCommit(args, "branch1", Optional.of("1"));
    // commitB
    verify(git).setNote("reviewid", "2");
    verify(git).setNote("reviewlasthash", "tree2");
  }

  @Test
  public void updateFirstRbIfItsChanged() {
    // given we want to review two commits
    when(git.getCurrentBranch()).thenReturn("branch1");
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA", "commitB").toList());
    // and the first one already has an id but has a new tree hash
    when(git.getNote("reviewid")).thenReturn(Optional.of("1"), Optional.empty());
    when(git.getNote("reviewlasthash")).thenReturn(Optional.of("tree1"), Optional.empty());
    when(git.getCurrentTreeHash()).thenReturn("tree1b", "tree2");
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.empty())).thenReturn("1");
    when(rb.createNewRbForCurrentCommit(args, "branch1", Optional.of("1"))).thenReturn("2");
    // when ran
    b.run(args);
    // then we post a new RB for the 2nd commit
    verify(git).getCurrentBranch();
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).cherryPick("commitB");
    verify(git, atLeast(2)).getNote("reviewid");
    verify(git, atLeast(2)).getNote("reviewlasthash");
    verify(git, atLeast(2)).getCurrentTreeHash();
    verify(rb).updateRbForCurrentCommit(args, "1", Optional.empty());
    verify(rb).createNewRbForCurrentCommit(args, "branch1", Optional.of("1"));
    // and update both commits' notes
    verify(git).setNote("reviewlasthash", "tree1b");
    verify(git).setNote("reviewid", "2");
    verify(git).setNote("reviewlasthash", "tree2");
  }

  @Test
  public void dcommitTwoCommits() {
    // given we want to dcommit two new commits
    when(git.getRevisionsFromOriginMaster()).thenReturn(Seq.of("commitA", "commitB").toList());
    when(git.getNote("reviewid")).thenReturn(Optional.of("1"), Optional.of("2"));
    // when ran
    b.run(new DCommitArgs());
    // then we dcommit each commit
    verify(git).getRevisionsFromOriginMaster();
    verify(git).resetHard("commitA");
    verify(git).cherryPick("commitB");
    verify(git, atLeast(2)).getNote("reviewid");
    verify(rb).dcommit("1");
    verify(rb).dcommit("2");
  }

  @Test
  public void setupGitNotes() {
    b.ensureGitNotesConfigured();
    verify(git, atLeast(2)).getMultipleValueConfig("notes.displayRef");
    verify(git, atLeast(2)).getMultipleValueConfig("notes.rewriteRef");
    verify(git).addMultipleValueConfig("notes.displayRef", "ref/notes/reviewid");
    verify(git).addMultipleValueConfig("notes.displayRef", "ref/notes/reviewlasthash");
    verify(git).addMultipleValueConfig("notes.rewriteRef", "ref/notes/reviewid");
    verify(git).addMultipleValueConfig("notes.rewriteRef", "ref/notes/reviewlasthash");
  }
}
