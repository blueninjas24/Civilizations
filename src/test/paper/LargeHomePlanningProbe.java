/** Real bed-height settlement center and an eight-person family must yield the full-height eight-bed proposal. */
public final class LargeHomePlanningProbe extends AutomaticHomePlanningProbe {
    @Override protected int familyMembers(){return 8;}
    @Override protected String expectedTemplate(){return "family_home_eight";}
}
