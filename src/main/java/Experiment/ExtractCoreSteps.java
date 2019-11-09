package Experiment;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.entities.StructureEntityVersion;
import core.Enums.Constants;
import core.Module;
import core.ast.ProjectInformation;
import core.git.ChangedFile;
import core.impactanalysis.Impact;
import core.stereotype.stereotyped.StereotypeIdentifier;
import core.stereotype.stereotyped.StereotypedCommit;
import core.stereotype.stereotyped.StereotypedElement;
import core.stereotype.stereotyped.StereotypedMethod;
import core.stereotype.taxonomy.CommitStereotype;
import core.summarizer.*;
import core.util.Utils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ExtractCoreSteps {

    public static ChangedFile.TypeChange defaultTypeChange = ChangedFile.TypeChange.MODIFIED;

    public static void main(String[] args) {

        /**
         * 由于我认为SummarizeChanges是核心类，因此除了该类中使用到的变量名和源代码一模一样以外
         * 其余的变量名不论是否重名，都增加 _in_{ClassName} 的后缀
         */

        try {

            /**
             * Step 1 : 对应SummarizeChanges类的初始化部分
             */
            Set<ChangedFile> differences_in_Main = new TreeSet<ChangedFile>();
            String pathB = "R:\\InteliJGit\\CommitSummarizer\\diff\\B@TagGroup.java";
            ChangedFile fileB = new ChangedFile(pathB, defaultTypeChange.name(), pathB);
            fileB.setAbsolutePath(pathB);
            differences_in_Main.add(fileB);

            /**
             * Step 2 : 对应Main.main中对changes的初始化部分
             */
            ChangedFile[] changes_in_Main = new ChangedFile[differences_in_Main.size()];
            differences_in_Main.toArray(changes_in_Main);

            /**
             * Step 3 : 对应SummarizeChanges中的summarize方法部分
             * Step 3.1 : 对应initSummary方法部分
             */
            ChangedFile[] differences = changes_in_Main;
            List<StereotypeIdentifier> identifiers = new ArrayList<StereotypeIdentifier>();
            SortedMap<String, StereotypeIdentifier> summarized = new TreeMap<String, StereotypeIdentifier>();
            LinkedList<ChangedFile> modifiedFiles = new LinkedList<>();
            List<Module> modules = new ArrayList<>();
            String summary = Constants.EMPTY_STRING;
            StereotypeIdentifier stereotypeIdentifier = new StereotypeIdentifier();
            StringBuilder comment = new StringBuilder();
            boolean filtering = false; double filterFactor = 0.5; //暂时不考虑过滤问题

            String currentPackage = Constants.EMPTY_STRING;

            /**
             * Step 3.2 : 对应analyzeForShell部分
             * 其中删去了大量的if-else，因为可以保证输入是java文件，且TypeChange是MODIFIED
             */
            //analyzeForShell();
            for (final ChangedFile file : differences) {
                StereotypeIdentifier identifier = null;

                //identifier = identifyStereotypes(file, file.getChangeType());
                /**
                 * Step 3.2.1 : 对应identifyStereotypes方法部分
                 */
                //getAddedStereotypeIdentifier(file);
                /**
                 * Step 3.2.1.1 : 对应getAddedStereotypeIdentifier方法部分
                 * 暂时不知道如何模拟其步骤
                 */

                stereotypeIdentifier.identifyStereotypes();
                stereotypeIdentifier.setScmOperation(file.getChangeType());
                stereotypeIdentifier.setChangedFile(file);

                identifiers.add(stereotypeIdentifier);

                identifier = stereotypeIdentifier;

                if (identifier != null) {
                    /**
                     * Step 3.2.2 : 对应summarizeType方法部分
                     */
                    if (identifier.getStereotypedElements().size() == 0) {
                        //typesProblem.add(identifier);
                        System.out.println("Stop here : typesProblem.add(identifier)");
                    }
                    int i = 0;
                    for (StereotypedElement element : identifier.getStereotypedElements()) {
                        SummarizeType summarizeType = new SummarizeType(element, identifier, differences);
                        if (i > 0) {
                            summarizeType.setLocal(true);
                        } else {
                            summarizeType.setLocal(false);
                        }
                        String key = element.getQualifiedName();
                        if (!key.contains(".")) {
                            key = identifier.getParser().getCompilationUnit().getPackage().getName() +
                                    "." + element.getQualifiedName();
                        }
                        if (!summarized.containsKey(key) && !summarized.containsValue(identifier)) {
                            summarized.put(key, identifier);
                        }
                        i++;
                    }
                }

            }

            /**
             * Step 3.2.3 : 对应composeCommitMessage方法部分
             */
            //composeCommitMessage();
            String currentPackage_in_cCM = Constants.EMPTY_STRING;
            StringBuilder desc = new StringBuilder();

            // i表示步骤
            int i = 1;
            int j = 1;

            /**
             * 由于暂时不是很清楚计算impact的具体流程如何模拟
             * 以下涉及到的部分先跳过
             */
            //Impact impact = new Impact(identifiers);
            //impact.setProject(ProjectInformation.getProject(ProjectInformation.getSelectedProject()));
            //impact.calculateImpactSet();


            // 先获取所有新的模块，填入modules列表
            //getNewModules();
            /**
             * Step 3.2.3.1 : 对应getNewModules方法部分
             */
            for (StereotypeIdentifier identifier : identifiers) {
                IType[] allTypes = identifier.getCompilationUnit().getAllTypes();

                for (IType iType : allTypes) {
                    /**
                     * Step 3.2.3.1.1 : 对应createModuleFromPackageElement方法部分
                     */
                    String packageName = iType.getPackageFragment().getElementName();
                    String extractedName = packageName.substring(
                            packageName.lastIndexOf(".") + 1, packageName.length());
                    Module module = new Module();
                    module.setModuleName(extractedName);
                    module.setPackageName(packageName);

                    if (!modules.contains(module)) {
                        modules.add(module);
                    }
                }
            }

            // 再描述上述新模块，完善commit message
            //describeNewModules(desc);
            /**
             * Step 3.2.3.2 : 对应describeNewModules方法部分
             */
            if(modules != null && modules.size() == 0) {
                System.out.println("Step describeNewModules not working");
            }
            else {
                StringBuilder descTmp = new StringBuilder(Constants.EMPTY_STRING);
                String connector = (modules.size() == 1) ? " this new module" : " these new modules";
                descTmp.append("The commit includes" + connector + ": \n\n");
                for (Module module : modules) {
                    if (!descTmp.toString().contains("\t- " + module.getModuleName() + Constants.NEW_LINE)) {
                        descTmp.append("\t- " + module.getModuleName() + Constants.NEW_LINE);
                    }
                }
                descTmp.append(Constants.NEW_LINE);
                desc.append(descTmp);
            }

            for (Map.Entry<String, StereotypeIdentifier> identifier : summarized.entrySet()) {
                StringBuilder descTmp = new StringBuilder(Constants.EMPTY_STRING);
                StereotypeIdentifier calculated = identifiers.get(identifiers.indexOf(identifier.getValue()));
//                if (filtering && calculated != null && calculated.getImpactPercentaje() <= (filterFactor)) {
//                    continue;
//                }

                /**
                 * i表示步骤
                 */
                if (i == 1) {
                    desc.append(" This change set is mainly composed of:  \n\n");
                }

                /**
                 * 根据package，来排序生成1. 2. 3. 等一级标题
                 * 生成一级标题的一句话
                 */
                if (currentPackage_in_cCM.trim().equals(Constants.EMPTY_STRING)) {
                    currentPackage_in_cCM = identifier.getValue().getParser().getCompilationUnit().getPackage().getName().getFullyQualifiedName();
                    desc.append(i + ". Changes to package " + currentPackage + ":  \n\n");
                    i++;
                } else if (!currentPackage_in_cCM.equals(identifier.getValue().getParser().getCompilationUnit().getPackage().getName().getFullyQualifiedName())) {
                    String[] lines = desc.toString().trim().split("\\n");
                    if (lines != null && lines.length > 0) {
                        String lastLine = lines[lines.length - 1];
                        if (lastLine.contains("Changes to package " + currentPackage_in_cCM)) {
                            lines[lines.length - 1] = "\n\n";
                            i--;
                        }
                    }
                    currentPackage_in_cCM = identifier.getValue().getParser().getCompilationUnit().getPackage().getName().getFullyQualifiedName();
                    desc.append(i + ". Changes to package " + currentPackage_in_cCM + ":  \n\n");
                    j = 1;
                    i++;
                }

                /**
                 * Step 3.2.3.3 : 对应modificationDescriptor.extractDifferences部分
                 * 该部分就是将git替换为直接的A文件的地方
                 */

                /**
                 * 每个包中的修改信息，1.1. 等二级子标题
                 */
                ModificationDescriptor modificationDescriptor = new ModificationDescriptor();
                modificationDescriptor.setDifferences(differences);
                modificationDescriptor.setFile(identifier.getValue().getChangedFile());

                FileDistiller distiller_in_ModificationDescriptor
                        = ChangeDistiller.createFileDistiller(ChangeDistiller.Language.JAVA);

                /**
                 * Step 3.2.3.3.1 : 对应compareModified方法部分
                 */
                File previousType = null;
                File currentType = null;

                //这就是设置A文件的地方
                previousType = new File("R:\\InteliJGit\\CommitSummarizer\\diff\\A@TagGroup.java");
                currentType = new File(fileB.getAbsolutePath());
                distiller_in_ModificationDescriptor.extractClassifiedSourceCodeChanges(previousType, currentType);

                modificationDescriptor
                        .setChanges(distiller_in_ModificationDescriptor.getSourceCodeChanges());

                modificationDescriptor.extractModifiedMethods();
                modificationDescriptor.describe(i, j, descTmp);

                if (!descTmp.toString().equals(Constants.EMPTY_STRING)) {
                    desc.append(descTmp.toString());
                    j++;
                }
            }

            //createGeneralDescriptor(desc, isInitialCommit);
            /**
             * Step 3.2.3 : 对应createGeneralDescriptor方法部分
             * 这一部分也不知道该怎么改，因为也涉及Git对象
             */
//            CommitGeneralDescriptor generalDescriptor = new CommitGeneralDescriptor();
//            generalDescriptor.setDifferences(differences);
//            generalDescriptor.setInitialCommit(true);
//            desc.insert(0, generalDescriptor.describe());

            //desc.insert(0, summarizeCommitStereotype());
            /**
             * Step 3.2.4 : 对应summarizeCommitStereotype方法部分
             */
            List<StereotypedMethod> methods = new ArrayList<StereotypedMethod>();
            String result = Constants.EMPTY_STRING;

            for(StereotypeIdentifier identifier : identifiers) {
                for(StereotypedElement element : identifier.getStereotypedElements()) {
                    List<StructureEntityVersion> modifiedMethods = identifier.getChangedFile().getModifiedMethods();
                    if (modifiedMethods != null) {
                        for (StructureEntityVersion structureEntityVersion : modifiedMethods) {

                            /**
                             * Step 3.2.4.1 : 对应getStereotypeElementFromName方法部分
                             */
                            StereotypedElement sresult =  null;
                            if(element.getStereoSubElements() != null) {
                                for (StereotypedElement stereotyped : element.getStereoSubElements()) {
                                    if(stereotyped.getFullyQualifiedName().equals(structureEntityVersion.getJavaStructureNode().getFullyQualifiedName()) ||
                                            structureEntityVersion.getJavaStructureNode().getFullyQualifiedName().endsWith(stereotyped.getFullyQualifiedName())) {
                                        sresult = stereotyped;
                                        break;
                                    }
                                }
                            }

                            StereotypedElement stereotypedMethod = sresult;

                            if (stereotypedMethod != null) {
                                methods.add((StereotypedMethod) stereotypedMethod);
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

            desc.insert(0, result);

//            if (isInitialCommit) {
//                desc.insert(0, "Initial commit. ");
//            } else {
//                desc.insert(0, "BUG - FEATURE: <type-ID> \n\n");
//            }

            String[] lines = desc.toString().trim().split("\\n");
            if (lines != null && lines.length > 0) {
                String lastLine = lines[lines.length - 1];
                if (lastLine.contains("Changes to package " + currentPackage_in_cCM)) {
                    lines[lines.length - 1] = "\n\n";
                }
            }

            summary = desc.toString();

            System.out.println(summary);

        }catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
