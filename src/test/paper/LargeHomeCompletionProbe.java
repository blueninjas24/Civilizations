/** Eight-person family, eight actual beds,167 lit native blocks and real4870-unit supplier payment. */
public final class LargeHomeCompletionProbe extends CivHomeCompletionProbe {
    @Override protected boolean automaticMode(){return true;}
    @Override protected String templateId(){return "family_home_eight";}
    @Override protected int familyMembers(){return 8;}
    @Override protected int homeBeds(){return 8;}
    @Override protected long expectedCost(){return 4870;}
    @Override protected long initialStock(){return 64;}
}
