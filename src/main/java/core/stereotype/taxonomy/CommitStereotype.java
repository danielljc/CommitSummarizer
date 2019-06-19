package core.stereotype.taxonomy;


public enum CommitStereotype implements CodeStereotype {
    STRUCTURE_MODIFIER("STRUCTURE_MODIFIER", 0), 
    STATE_ACCESS_MODIFIER("STATE_ACCESS_MODIFIER", 1), 
    STATE_UPDATE_MODIFIER("STATE_UPDATE_MODIFIER", 2), 
    BEHAVIOR_MODIFIER("BEHAVIOR_MODIFIER", 3), 
    OBJECT_CREATION_MODIFIER("OBJECT_CREATION_MODIFIER", 4), 
    RELATIONSHIP_MODIFIER("RELATIONSHIP_MODIFIER", 5), 
    CONTROL_MODIFIER("CONTROL_MODIFIER", 6), 
    LARGE_MODIFIER("LARGE_MODIFIER", 7), 
    LAZY_MODIFIER("LAZY_MODIFIER", 8), 
    DEGENERATE_MODIFIER("DEGENERATE_MODIFIER", 9), 
    SMALL_MODIFIER("SMALL_MODIFIER", 10); 
    
    private CommitStereotype(final String s, final int n) {
    }
}
