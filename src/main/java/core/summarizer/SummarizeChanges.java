package core.summarizer;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.entities.StructureEntityVersion;
import core.Enums.Constants;
import core.Module;
import core.ast.ProjectInformation;
import core.git.ChangedFile;
import core.git.ChangedFile.TypeChange;
import core.impactanalysis.Impact;
import core.stereotype.stereotyped.StereotypeIdentifier;
import core.stereotype.stereotyped.StereotypedCommit;
import core.stereotype.stereotyped.StereotypedElement;
import core.stereotype.stereotyped.StereotypedMethod;
import core.stereotype.taxonomy.CommitStereotype;
import core.stereotype.taxonomy.MethodStereotype;
import core.textgenerator.phrase.util.CompilationUtils;
//import core.ui.IDialog;
import core.util.Utils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.swt.widgets.Display;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class SummarizeChanges {
	private Git git;
	private StereotypeIdentifier stereotypeIdentifier;
	private List<Module> modules;
	private List<StereotypeIdentifier> identifiers;
	private StringBuilder comment = new StringBuilder();
	private ChangedFile[] differences;
//	private IDialog changedListDialog;
	private SortedMap<String, StereotypeIdentifier> summarized = new TreeMap<String, StereotypeIdentifier>();
	private FileDistiller distiller; 
	private LinkedList<ChangedFile> modifiedFiles;
	private LinkedList<ChangedFile> otherFiles;
	private List<StereotypeIdentifier> typesProblem;
	private String projectPath;
	private boolean filtering;
	private double filterFactor;
	private String summary;
	private String olderVersionId;
	private String newerVersionId;
	private String projectName;
	private IResource projectAsResource;
	
	public SummarizeChanges(Git git, boolean filtering, double filterFactor, String olderVersionId, String newerVersionId) {
		super();
		this.git = git;
		this.filterFactor = filterFactor;
		this.filtering = filtering;
		this.stereotypeIdentifier = new StereotypeIdentifier();
		this.identifiers = new ArrayList<StereotypeIdentifier>();
		this.olderVersionId = olderVersionId;
		this.newerVersionId = newerVersionId;
		this.distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
	}
	
	public void initSummary(final ChangedFile[] differences) {
		this.differences = differences;
		this.identifiers = new ArrayList<StereotypeIdentifier>();
		this.summarized = new TreeMap<String, StereotypeIdentifier>();
		this.modifiedFiles = new LinkedList<>();
		this.otherFiles = new LinkedList<>();
		this.typesProblem = new LinkedList<>();
		this.modules = new ArrayList<>();
		this.summary = Constants.EMPTY_STRING;

		/*if(changedListDialog != null) {
			changedListDialog.getEditor().getText().setText(Constants.EMPTY_STRING);
		}*/
		removeCreatedPackages();
	}

	@SuppressWarnings("unused")
	public void summarize(final ChangedFile[] differences) {
		initSummary(differences);
		String currentPackage = Constants.EMPTY_STRING;
		
		if(null != projectPath && !projectPath.isEmpty()) {
			analyzeForShell();
		} else {
			analyzeForPlugin();
		}
	}
	
	private void analyzeForShell() {
		for (final ChangedFile file : differences) {
			StereotypeIdentifier identifier = null;
			try {
				if (file.getAbsolutePath().endsWith(Constants.JAVA_EXTENSION)) {
					if (file.getChangeType().equals(TypeChange.UNTRACKED.name()) || file.getChangeType().equals(TypeChange.ADDED.name())) {
						if (file.getAbsolutePath().endsWith(Constants.JAVA_EXTENSION)) {
							identifier = identifyStereotypes(file,file.getChangeType());
						}
					} else if (file.getChangeType().equals(
							TypeChange.REMOVED.name())) {
						if (file.getAbsolutePath().endsWith(Constants.JAVA_EXTENSION)) {
							identifier = identifyStereotypes(file, file.getChangeType());
						}
					} else if (file.getChangeType().equals(TypeChange.MODIFIED.name())) {
						if (file.getAbsolutePath().endsWith(Constants.JAVA_EXTENSION)) {
							identifier = identifyStereotypes(file,file.getChangeType());
						}
					}
				} else {
					otherFiles.add(file);
				}

				if (identifier != null) {
					summarizeType(identifier);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		composeCommitMessage();
	}

	private void analyzeForPlugin() {
		Job job = new Job("Calculating method and types stereotypes") {
			@Override
			protected IStatus run(IProgressMonitor externalMonitor) {
				for (final ChangedFile file : differences) {
					Job internalJob = new Job("Calculating stereotype for " + file.getName()) {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							StereotypeIdentifier identifier = null;
							try {
								if (file.getAbsolutePath().endsWith(Constants.JAVA_EXTENSION)) {
									if (file.getChangeType().equals(TypeChange.UNTRACKED.name()) || file.getChangeType().equals(TypeChange.ADDED.name())) {
										if (file.getAbsolutePath().endsWith(Constants.JAVA_EXTENSION)) {
											monitor.subTask("Identifying stereotypes for " + file.getName());
											identifier = identifyStereotypes(file,file.getChangeType());
										}
									} else if (file.getChangeType().equals(
											TypeChange.REMOVED.name())) {
										if (file.getAbsolutePath().endsWith(Constants.JAVA_EXTENSION)) {
											monitor.subTask("Identifying stereotypes for " + file.getName());
											identifier = identifyStereotypes(file, file.getChangeType());
										}
									} else if (file.getChangeType().equals(TypeChange.MODIFIED.name())) {
										if (file.getAbsolutePath().endsWith(Constants.JAVA_EXTENSION)) {
											monitor.subTask("Identifying stereotypes for "+ file.getName());
											identifier = identifyStereotypes(file,file.getChangeType());
										}
									}
								} else {
									otherFiles.add(file);
								}

								if (identifier != null) {
									monitor.subTask("Describing type " + file.getName());
									summarizeType(identifier);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}

							return Status.OK_STATUS;
						}
					};
					internalJob.schedule();
					try {
						internalJob.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				return Status.OK_STATUS;
			}
		};
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				updateTextInputDescription();
			}
		});
		job.schedule();
	}
	
	public void describeInitialCommit() {
		StringBuilder desc = new StringBuilder("Initial commit. "); 
		CommitGeneralDescriptor generalDescriptor = new CommitGeneralDescriptor();
		generalDescriptor.setDifferences(differences);
		generalDescriptor.setGit(git);
		desc.append(generalDescriptor.describe());
		
		/*if(changedListDialog != null) {
			changedListDialog.getEditor().getText().setText(desc.toString());
		}*/
	}

	public void updateTextInputDescription() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				composeCommitMessage();
			}
		});

	}

	/**
	 * 生成commit message的核心文件
	 */
	protected void composeCommitMessage() {
		String currentPackage = Constants.EMPTY_STRING;
		StringBuilder desc = new StringBuilder();

		// i表示步骤
		int i = 1;
		int j = 1;
		// 判断该git是否为init提交（可省略）
		boolean isInitialCommit = Utils.isInitialCommit(git); 
		
		if(null == projectPath) {
			Impact impact = new Impact(identifiers);
			impact.setProject(ProjectInformation.getProject(ProjectInformation.getSelectedProject()));
			//impact.calculateImpactSet();
		}
		
		if(isInitialCommit) {
			// 先获取所有新的模块，填入modules列表
			getNewModules();
			// 再描述上述新模块，完善commit message
			describeNewModules(desc);
		} 
		
		for(Entry<String, StereotypeIdentifier> identifier : summarized.entrySet()) {
			StringBuilder descTmp = new StringBuilder(Constants.EMPTY_STRING);
			StereotypeIdentifier calculated = identifiers.get(identifiers.indexOf(identifier.getValue()));
			if(filtering && calculated != null && calculated.getImpactPercentaje() <= (filterFactor) ) {
				continue;
			}

			/**
			 * i表示步骤
			 */
			if(i == 1) {
				desc.append(" This change set is mainly composed of:  \n\n");
			}

			/**
			 * 根据package，来排序生成1. 2. 3. 等一级标题
			 * 生成一级标题的一句话
			 */
			if(currentPackage.trim().equals(Constants.EMPTY_STRING)) {
				currentPackage = identifier.getValue().getParser().getCompilationUnit().getPackage().getName().getFullyQualifiedName();
				desc.append(i + ". Changes to package " + currentPackage + ":  \n\n");
				i++;
			} else if(!currentPackage.equals(identifier.getValue().getParser().getCompilationUnit().getPackage().getName().getFullyQualifiedName())) {
				String[] lines = desc.toString().trim().split("\\n");
				if(lines != null && lines.length > 0) {
					String lastLine = lines[lines.length - 1];
					if(lastLine.contains("Changes to package " + currentPackage)) {
						lines[lines.length - 1] = "\n\n";
						//desc = new StringBuilder(StringUtils.join(lines, "\\n)"));
						i--;
					}
				}
				currentPackage = identifier.getValue().getParser().getCompilationUnit().getPackage().getName().getFullyQualifiedName();
				desc.append(i + ". Changes to package " + currentPackage + ":  \n\n");
				j = 1;
				i++;
			}

			/**
			 * 每个包中的修改信息，1.1. 等二级子标题
			 */
			if(identifier.getValue().getScmOperation().equals(TypeChange.MODIFIED.toString())) {
				ModificationDescriptor modificationDescriptor = new ModificationDescriptor();
				modificationDescriptor.setDifferences(differences);
				modificationDescriptor.setFile(identifier.getValue().getChangedFile());
				modificationDescriptor.setGit(getGit());
				if(olderVersionId == null) {
					modificationDescriptor.extractDifferences(identifier.getValue().getChangedFile(), git);
				} else {
					modificationDescriptor.extractDifferencesBetweenVersions(identifier.getValue().getChangedFile(), git,
							olderVersionId, newerVersionId);
				}
				
				modificationDescriptor.extractModifiedMethods();
				modificationDescriptor.describe(i, j, descTmp);
			} else {
				if(!identifier.getValue().getChangedFile().isRenamed()) {
					descTmp.append((i - 1) + "." + j + ". " + identifier.getValue().toString());
				} else {
					descTmp.append((i - 1) + "." + j + ". " + "Rename type " + identifier.getValue().getChangedFile().getRenamedPath().substring(identifier.getValue().getChangedFile().getRenamedPath().lastIndexOf("/") + 1).replace(Constants.JAVA_EXTENSION, Constants.EMPTY_STRING) + " with " + identifier.getValue().getChangedFile().getName().replace(Constants.JAVA_EXTENSION, "\n\n"));
				}
			}


			if(!descTmp.toString().equals(Constants.EMPTY_STRING)) {
				desc.append(descTmp.toString());
				j++;
			}
		}
		
		createGeneralDescriptor(
				desc, isInitialCommit);
		//Commit stereotype description
		desc.insert(0, summarizeCommitStereotype());
		
		if(isInitialCommit) {
			desc.insert(0, "Initial commit. "); 
		} else { 
			desc.insert(0, "BUG - FEATURE: <type-ID> \n\n");
		}
		
		String[] lines = desc.toString().trim().split("\\n");
		if(lines != null && lines.length > 0) {
			String lastLine = lines[lines.length - 1];
			if(lastLine.contains("Changes to package " + currentPackage)) {
				lines[lines.length - 1] = "\n\n";
				//desc = new StringBuilder(StringUtils.join(lines, "\\n"));
			}
		}
		/*if(changedListDialog != null) {
			changedListDialog.getEditor().getText().setText(desc.toString());
			changedListDialog.updateSignatureCanvas();
			changedListDialog.updateMessage();
		} */
		this.setSummary(desc.toString());
		removeCreatedPackages();
	}

	private CommitGeneralDescriptor createGeneralDescriptor(StringBuilder desc,
			boolean isInitialCommit) {
		CommitGeneralDescriptor generalDescriptor = new CommitGeneralDescriptor();
		generalDescriptor.setDifferences(differences);
		generalDescriptor.setInitialCommit(isInitialCommit);
		generalDescriptor.setGit(git);
		desc.insert(0, generalDescriptor.describe());
		return generalDescriptor;
	}
	
	private void setSummary(String summary) {
		this.summary = summary;
		
	}
	
	public String getSummary() {
		return this.summary;
	}

	/**
	 * 字符串：描述新的模块
	 * @param desc
	 */
	protected void describeNewModules(StringBuilder desc) {
		if(modules != null && modules.size() == 0) {
			return;
		}
		StringBuilder descTmp = new StringBuilder(Constants.EMPTY_STRING);
		String connector = (modules.size() == 1) ? " this new module" : " these new modules";
		descTmp.append("The commit includes" + connector + ": \n\n");
		for (Module module : modules) {
			if(!descTmp.toString().contains("\t- " + module.getModuleName() + Constants.NEW_LINE)) {
				descTmp.append("\t- " + module.getModuleName() + Constants.NEW_LINE);
			}
		}
		descTmp.append(Constants.NEW_LINE);

		// 将新模块描述的字符串，append到整个commit message中
		desc.append(descTmp);
	}

	/**
	 * 获取所有的新模块，更新成员变量models列表
	 */
	protected void getNewModules() {
		for (StereotypeIdentifier identifier : identifiers) {
			try {
				IType[] allTypes = identifier.getCompilationUnit().getAllTypes();
				
				for (IType iType : allTypes) {
					Module module = createModuleFromPackageElement(iType);
					if(!modules.contains(module)) {
						modules.add(module);
					}
				}
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private Module createModuleFromPackageElement(IType iType) {
		String packageName = iType.getPackageFragment().getElementName();
		String extractedName = packageName.substring(
				packageName.lastIndexOf(".") + 1, packageName.length());
		Module module = new Module();
		module.setModuleName(extractedName);
		module.setPackageName(packageName);
		return module;
	}
	
	protected void removeCreatedPackages() {
		IFolder folder = null;
		String tmpFolderPath = "src/commsummtmp";
		/*if(null != changedListDialog && null != changedListDialog.getSelection()) {
			folder = ((IJavaProject) changedListDialog.getSelection()).getProject().getFolder(tmpFolderPath);
			try {
				folder.delete(true, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		} else if(null != projectPath && !projectPath.isEmpty()) {
			File tmpFolder = new File(projectPath + System.getProperty("file.separator") + tmpFolderPath);
			if(tmpFolder.exists()) {
				tmpFolder.delete();
			}
		}*/
	}
	
	public void compareModified(ChangedFile file) {
		File previousType = null;
		File currentType = null;
		try {
			if(olderVersionId != null && !olderVersionId.equals(Constants.EMPTY_STRING)) { 
				previousType = Utils.getFileContentOfCommitID(file.getPath(), getGit().getRepository(), olderVersionId);
			} else {
				previousType = Utils.getFileContentOfLastCommit(file.getPath(), getGit().getRepository());
			}
			currentType = new File(file.getAbsolutePath());
			distiller.extractClassifiedSourceCodeChanges(previousType, currentType);
		} catch (RevisionSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AmbiguousObjectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IncorrectObjectTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void summarizeType(StereotypeIdentifier identifier) {
		if(identifier.getStereotypedElements().size() == 0) {
			typesProblem.add(identifier);
		}
		int i = 0;
		for(StereotypedElement element : identifier.getStereotypedElements()) {
				SummarizeType summarizeType = new SummarizeType(element, identifier, differences);
				if(i > 0) {
					summarizeType.setLocal(true);
				} else {
					summarizeType.setLocal(false);
				}
				if(!identifier.getScmOperation().equals(TypeChange.MODIFIED.toString())) {
					summarizeType.generate();
					if(null != summarizeType.getBuilder()) {
						identifier.getBuilder().append(summarizeType.getBuilder().toString());
					}
				}
				String key = element.getQualifiedName();
				if(!key.contains(".")) {
					key = identifier.getParser().getCompilationUnit().getPackage().getName() + "." + element.getQualifiedName();
				}
				if(!summarized.containsKey(key) && !summarized.containsValue(identifier)) {
					summarized.put(key, identifier);
				}
				i++;
		}
	}
	
	@SuppressWarnings("unchecked")
	public String summarizeCommitStereotype() {
		List<StereotypedMethod> methods = new ArrayList<StereotypedMethod>();
		String result = Constants.EMPTY_STRING;
		
		for(StereotypeIdentifier identifier : identifiers) {
			for(StereotypedElement element : identifier.getStereotypedElements()) {
				if(!identifier.getScmOperation().equals(TypeChange.MODIFIED.name()) && !identifier.getChangedFile().isRenamed()) {
					methods.addAll((Collection<? extends StereotypedMethod>) element.getStereoSubElements());
				} else {
					List<StructureEntityVersion> modifiedMethods = identifier.getChangedFile().getModifiedMethods();
					if (modifiedMethods != null) {
						for (StructureEntityVersion structureEntityVersion : modifiedMethods) {
							StereotypedElement stereotypedMethod = getStereotypedElementFromName(
									element,
									structureEntityVersion);
							if(stereotypedMethod != null) {
								methods.add((StereotypedMethod) stereotypedMethod);
							}
						}
					}
				}
			}
		}
		
		if(methods.size() > 0) {
			StereotypedCommit stereotypedCommit = new StereotypedCommit(methods);
			stereotypedCommit.buildSignature();
			CommitStereotype stereotype = stereotypedCommit.findStereotypes();
			
			if(stereotype != null) {
				result = CommitStereotypeDescriptor.describe(stereotypeIdentifier.getCompilationUnit() ,stereotypedCommit);
			} 
			/*if(changedListDialog != null) {
				changedListDialog.setSignatureMap(stereotypedCommit.getSignatureMap());
			}*/
		} else {
			/*if(changedListDialog != null) {
				changedListDialog.setSignatureMap(new TreeMap<MethodStereotype, Integer>());
			}*/
		}
		
		return result;
	}
	
	public StereotypedElement getStereotypedElementFromName(StereotypedElement element, StructureEntityVersion searchedElement) {
		StereotypedElement result =  null;
		if(element.getStereoSubElements() != null) {
			for (StereotypedElement stereotyped : element.getStereoSubElements()) { 
				if(stereotyped.getFullyQualifiedName().equals(searchedElement.getJavaStructureNode().getFullyQualifiedName()) || 
						searchedElement.getJavaStructureNode().getFullyQualifiedName().endsWith(stereotyped.getFullyQualifiedName())) {
					result = stereotyped;
					break;
				}
			}
		}
		return result;
	}
	
	public StereotypeIdentifier identifyStereotypes(final ChangedFile file, String scmOperation) {
		if(scmOperation.equals(TypeChange.ADDED.toString()) ||
				scmOperation.equals(TypeChange.UNTRACKED.toString()) || 
				scmOperation.equals(TypeChange.MODIFIED.toString())) {
			/*Display.getDefault().asyncExec(new Runnable() {
				public void run() {*/
			getAddedStereotypeIdentifier(file);
				//}});
		} else if(scmOperation.equals(TypeChange.REMOVED.toString())) {
			stereotypeIdentifier = getRemovedStereotypeIdentifier(file);
		} 
		stereotypeIdentifier.identifyStereotypes();
		stereotypeIdentifier.setScmOperation(scmOperation);
		stereotypeIdentifier.setChangedFile(file);
		
		identifiers.add(stereotypeIdentifier);
		
		return stereotypeIdentifier;
	}
	
	public StereotypeIdentifier getAddedStereotypeIdentifier(ChangedFile file) {
		if(null == projectPath){
			projectName = ProjectInformation.getProject(ProjectInformation.getSelectedProject()).getName();
			projectAsResource = (null != projectAsResource) ? projectAsResource : ProjectInformation.getProject(ProjectInformation.getSelectedProject()).findMember(file.getPath().replaceFirst(projectName, Constants.EMPTY_STRING));
			IFile ifile = ProjectInformation.getSelectedProject().getWorkspace().getRoot().getFile(projectAsResource.getFullPath());
			stereotypeIdentifier = new StereotypeIdentifier((ICompilationUnit) JavaCore.create(ifile), 0, 0);
		} else {
			try {
				stereotypeIdentifier = new StereotypeIdentifier(new File(file.getAbsolutePath()));
			} catch (RevisionSyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
		return stereotypeIdentifier;
	}

	public StereotypeIdentifier getRemovedStereotypeIdentifier(ChangedFile file) {
		try {
			String removedFile = Constants.EMPTY_STRING;
			if(olderVersionId != null && !olderVersionId.equals(Constants.EMPTY_STRING)) { 
				removedFile = Utils.getStringContentOfCommitID(file.getPath(), getGit().getRepository(), olderVersionId);
			} else {
				removedFile = Utils.getStringContentOfLastCommit(file.getPath(), getGit().getRepository());
			}
			IPackageFragment pack = null;
			String packageName = Constants.EMPTY_STRING;
			packageName = "commsummtmp." + CompilationUtils.getPackageNameFromStringClass(removedFile);
			/*IFolder folder = ((IJavaProject) changedListDialog.getSelection()).getProject().getFolder("src");
			JavaCore.newProjectEntry(folder.getLocation());
			changedListDialog.getSelection().getPackageFragmentRoot(folder).createPackageFragment("commsummtmp", true, null);
			pack = changedListDialog.getSelection().getPackageFragmentRoot(folder).createPackageFragment(packageName, true, null);*/
			ICompilationUnit cu = pack.createCompilationUnit(file.getName(), removedFile,true, null);
			stereotypeIdentifier = new StereotypeIdentifier(cu, 0, 0);
		} catch (RevisionSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AmbiguousObjectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IncorrectObjectTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return stereotypeIdentifier;
	}
	
	public StereotypeIdentifier getModifiedStereotypeIdentifier(ChangedFile file) {
		try {
			String removedFile = Constants.EMPTY_STRING;
			if(olderVersionId != null && !olderVersionId.equals(Constants.EMPTY_STRING)) { 
				removedFile = Utils.getStringContentOfCommitID(file.getPath(), getGit().getRepository(), olderVersionId);
			} else {
				removedFile = Utils.getStringContentOfLastCommit(file.getPath(), getGit().getRepository());
			}
			IPackageFragment pack = null;
			String packageName = Constants.EMPTY_STRING;
			packageName = "commsummtmp." + CompilationUtils.getPackageNameFromStringClass(removedFile);
			/*IFolder folder = ((IJavaProject)changedListDialog.getSelection()).getProject().getFolder("src");
			pack = changedListDialog.getSelection().getPackageFragmentRoot(folder).createPackageFragment(packageName, true, null);*/
			ICompilationUnit cu = pack.createCompilationUnit(file.getName(), removedFile,true, null);
			stereotypeIdentifier = new StereotypeIdentifier(cu, 0, 0);
		} catch (RevisionSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AmbiguousObjectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IncorrectObjectTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return stereotypeIdentifier;
	}
	
	public Git getGit() {
		return git;
	}

	public void setGit(Git git) {
		this.git = git;
	}

	public StringBuilder getComment() {
		return comment;
	}

	public void setComment(StringBuilder comment) {
		this.comment = comment;
	}

	public List<StereotypeIdentifier> getIdentifiers() {
		return identifiers;
	}

	public void setIdentifiers(List<StereotypeIdentifier> identifiers) {
		this.identifiers = identifiers;
	}

/*	public void setChangedListDialog(IDialog changedListDialog) {
		this.changedListDialog = changedListDialog;
	}*/

	public LinkedList<ChangedFile> getModifiedFiles() {
		return modifiedFiles;
	}

	public void setModifiedFiles(LinkedList<ChangedFile> modifiedFiles) {
		this.modifiedFiles = modifiedFiles;
	}

	public List<Module> getModules() {
		return modules;
	}

	public void setModules(List<Module> modules) {
		this.modules = modules;
	}

	public String getProjectPath() {
		return projectPath;
	}

	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath;
	}

	public boolean isFiltering() {
		return filtering;
	}

	public void setFiltering(boolean filtering) {
		this.filtering = filtering;
	}

	public double getFilterFactor() {
		return filterFactor;
	}

	public void setFilterFactor(double filterFactor) {
		this.filterFactor = filterFactor;
	}
}
