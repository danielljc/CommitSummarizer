package core.dependencies;

import core.ast.ProjectInformation;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.search.*;
import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;

public class MethodDependencySummary extends DependencySummary {
	private String name;
	private boolean isConstructor;
	
	public MethodDependencySummary(String name) {
		setName(name); 
		this.setDependencies(new ArrayList<SearchMatch>());
		setProject(ProjectInformation.getProject(ProjectInformation.getSelectedProject()));
		
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				setProject(ProjectInformation.getProject(ProjectInformation.getSelectedProject()));
			}
		});
	}

	@Override
	public void find() {
		SearchEngine engine = new SearchEngine();
        IJavaSearchScope workspaceScope = null;
        
        if(getProject() != null) {
        	workspaceScope = SearchEngine.createJavaSearchScope(createSearchScope());
        } else {
        	workspaceScope = SearchEngine.createWorkspaceScope();
        }
        int constructor = IJavaSearchConstants.METHOD;
        if(isConstructor()) {
        	constructor = IJavaSearchConstants.CONSTRUCTOR;
        }
        
        if(null != getName() && !getName().isEmpty()) {
	        SearchPattern pattern = SearchPattern.createPattern(
	                		getName(),
	                		constructor,
	                        IJavaSearchConstants.REFERENCES,
	                        SearchPattern.R_EXACT_MATCH);
	        SearchParticipant[] participant = new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
	        try {
				engine.search(pattern, participant, workspaceScope, createSearchRequestor(), new NullProgressMonitor());
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
	}

	@Override
	public void generateSummary() {
		// TODO Auto-generated method stub
		
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isConstructor() {
		return isConstructor;
	}

	public void setConstructor(boolean isConstructor) {
		this.isConstructor = isConstructor;
	}
}
