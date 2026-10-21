/** Runs the same guarded/restored native home fixture through the ordinary automatic scheduler. */
public final class AutomaticHomeCompletionProbe extends CivHomeCompletionProbe {
    @Override protected boolean automaticMode(){return true;}
}
