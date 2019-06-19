package core.summarizer;

import core.dependencies.TypeDependencySummary;
import core.git.ChangedFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;

public class ImpactSetDescriptor {
	
	public static String describe(ICompilationUnit cu, ChangedFile[] differences, String operation) {
		
		TypeDependencySummary dependency = new TypeDependencySummary((IJavaElement) cu, operation);
		if(null != cu) {
			dependency.setDifferences(differences);
			dependency.find();
			dependency.generateSummary();
		}
		
		return dependency.toString();
	}
}
