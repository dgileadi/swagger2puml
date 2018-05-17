package com.kicksolutions.swagger.plantuml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.kicksolutions.swagger.plantuml.vo.ClassDiagram;
import com.kicksolutions.swagger.plantuml.vo.ClassMembers;
import com.kicksolutions.swagger.plantuml.vo.ClassRelation;
import com.kicksolutions.swagger.plantuml.vo.InterfaceDiagram;
import com.kicksolutions.swagger.plantuml.vo.MethodDefinitions;

import org.apache.commons.lang3.StringUtils;

import io.swagger.models.ArrayModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;

/**
 * @author MSANTOSH
 * @author David Gileadi
 *
 */
public class PlantUMLClassCodegen extends PlantUMLCodegen<ClassDiagram, ClassMembers, ClassRelation> {

    private boolean generateDefinitionModelOnly = false;
    private boolean includeCardinality = true;

    public static final String CARDINALITY_ONE_TO_MANY = "1..*";
    public static final String CARDINALITY_NONE_TO_MANY = "0..*";
    public static final String CARDINALITY_ONE_TO_ONE = "1..1";
    public static final String CARDINALITY_NONE_TO_ONE = "0..1";

    public PlantUMLClassCodegen(Swagger swagger, File targetLocation, boolean generateDefinitionModelOnly,
            boolean includeCardinality) {
        super(swagger, targetLocation);
		this.generateDefinitionModelOnly = generateDefinitionModelOnly;
		this.includeCardinality = includeCardinality;
    }

    /**
     *
     * @param swagger
     */
    @Override
    protected Map<String, Object> preprocessSwagger(Swagger swagger) {
        LOGGER.entering(LOGGER.getName(), "preprocessSwagger");

        Map<String, Object> additionalProperties = new TreeMap<String, Object>();

        additionalProperties.put("title", swagger.getInfo().getTitle());
        additionalProperties.put("version", swagger.getInfo().getVersion());

        List<ClassDiagram> diagrams = processSwaggerModels(swagger);
        additionalProperties.put("classDiagrams", diagrams);

        List<InterfaceDiagram> interfaceDiagrams = new ArrayList<InterfaceDiagram>();

        if (!generateDefinitionModelOnly) {
            interfaceDiagrams.addAll(processSwaggerPaths(swagger));
            additionalProperties.put("interfaceDiagrams", interfaceDiagrams);
        }

        additionalProperties.put("classRelations", getRelations(diagrams, interfaceDiagrams));

        LOGGER.exiting(LOGGER.getName(), "preprocessSwagger");

        return additionalProperties;
    }

	@Override
	protected ClassDiagram createDiagram(String className, Model modelObject, List<ClassMembers> members,
			List<ClassRelation> relations) {

        String superClass = getSuperClass(modelObject);

        return new ClassDiagram(className, modelObject.getDescription(), members,
                relations, isModelClass(modelObject), superClass);
	}

	@Override
    protected ClassMembers createMember(String name, String dataType, String className, Model modelObject) {
        String cardinality = null;
        if (className != null && includeCardinality) {
            if (isRequiredProperty(modelObject, name)) {
                cardinality = CARDINALITY_ONE_TO_MANY;
            } else {
                cardinality = CARDINALITY_NONE_TO_MANY;
            }
        }

        return new ClassMembers(dataType, name, className, cardinality);
	}

	@Override
    protected boolean addRelation(ClassMembers member, Model modelObject, List<ClassRelation> addTo) {

        boolean alreadyExists = false;

        for (ClassRelation relation : addTo) {

            if (relation.getTarget().equalsIgnoreCase(member.getClassName())) {
                alreadyExists = true;
            }
        }

        if (!alreadyExists) {
            String superClass = getSuperClass(modelObject);

            final ClassRelation relation;
            if (StringUtils.isNotEmpty(superClass)) {
                relation = new ClassRelation(member.getClassName(), true, false, member.getCardinality(), null);
            } else {
                relation = new ClassRelation(member.getClassName(), false, true, member.getCardinality(), null);
            }

            addTo.add(relation);
            return true;
        }

        return false;
	}

    /**
     *
     * @param classDiagrams
     * @param interfaceDiagrams
     * @return
     */
    private List<ClassRelation> getRelations(List<ClassDiagram> classDiagrams,
            List<InterfaceDiagram> interfaceDiagrams) {
        List<ClassRelation> relations = new ArrayList<ClassRelation>();
        relations.addAll(getAllModelRelations(classDiagrams));
        relations.addAll(getAllInterfacesRelations(interfaceDiagrams));

        return filterUnique(relations, false);
    }

    /**
     *
     * @param classDiagrams
     * @return
     */
    private List<ClassRelation> getAllModelRelations(List<ClassDiagram> classDiagrams) {
        List<ClassRelation> modelRelations = new ArrayList<ClassRelation>();

        for (ClassDiagram classDiagram : classDiagrams) {
            List<ClassRelation> classRelations = classDiagram.getChildClass();

            for (ClassRelation classRelation : classRelations) {
                classRelation.setSource(classDiagram.getClassName());
                modelRelations.add(classRelation);
            }
        }

        return modelRelations;
    }

    /**
     *
     * @param classDiagrams
     * @return
     */
    private List<ClassRelation> getAllInterfacesRelations(List<InterfaceDiagram> interfaceDiagrams) {
        List<ClassRelation> modelRelations = new ArrayList<ClassRelation>();

        for (InterfaceDiagram classDiagram : interfaceDiagrams) {
            List<ClassRelation> classRelations = classDiagram.getChildClass();

            for (ClassRelation classRelation : classRelations) {
                classRelation.setSource(classDiagram.getInterfaceName());
                modelRelations.add(classRelation);
            }
        }

        return modelRelations;
    }

    /**
     *
     * @param swagger
     * @return
     */
    private List<InterfaceDiagram> processSwaggerPaths(Swagger swagger) {
        LOGGER.entering(LOGGER.getName(), "processSwaggerPaths");
        List<InterfaceDiagram> interfaceDiagrams = new ArrayList<InterfaceDiagram>();
        Map<String, Path> paths = swagger.getPaths();

        for (Map.Entry<String, Path> entry : paths.entrySet()) {
            Path pathObject = entry.getValue();

            LOGGER.info("Processing Path --> " + entry.getKey());

            List<Operation> operations = pathObject.getOperations();
            String uri = entry.getKey();

            for (Operation operation : operations) {
                interfaceDiagrams.add(getInterfaceDiagram(operation, uri));
            }
        }

        LOGGER.exiting(LOGGER.getName(), "processSwaggerPaths");
        return interfaceDiagrams;
    }

    /**
     *
     * @param operation
     * @return
     */
    private InterfaceDiagram getInterfaceDiagram(Operation operation, String uri) {
        LOGGER.entering(LOGGER.getName(), "getInterfaceDiagram");

        InterfaceDiagram interfaceDiagram = new InterfaceDiagram();
        String interfaceName = getInterfaceName(operation.getTags(), operation, uri);
        String errorClassName = getErrorClassName(operation);
        interfaceDiagram.setInterfaceName(interfaceName);
        interfaceDiagram.setErrorClass(errorClassName);
        interfaceDiagram.setMethods(getInterfaceMethods(operation));
        interfaceDiagram.setChildClass(getInterfaceRelations(operation, errorClassName));

        LOGGER.exiting(LOGGER.getName(), "getInterfaceDiagram");
        return interfaceDiagram;
    }

    /**
     *
     * @param operation
     * @return
     */
    private List<ClassRelation> getInterfaceRelations(Operation operation, String errorClassName) {
        List<ClassRelation> relations = new ArrayList<ClassRelation>();
        relations.addAll(getInterfaceRelatedResponses(operation));
        relations.addAll(getInterfaceRelatedInputs(operation));
        if (StringUtils.isNotEmpty(errorClassName)) {
            relations.add(getErrorClass(errorClassName));
        }

        return filterUnique(relations, true);
    }

    /**
     *
     * @param relations
     * @return
     */
    private List<ClassRelation> filterUnique(List<ClassRelation> relations, boolean compareTargetOnly) {
        List<ClassRelation> uniqueList = new ArrayList<ClassRelation>();

        for (ClassRelation relation : relations) {
            if (!isTargetClassInMap(relation, uniqueList, compareTargetOnly)) {
                uniqueList.add(relation);
            }
        }

        return uniqueList;
    }

    /**
     *
     * @param className
     * @param relatedResponses
     * @return
     */
    private boolean isTargetClassInMap(ClassRelation sourceRelation, List<ClassRelation> relatedResponses,
            boolean considerTargetOnly) {
        for (ClassRelation relation : relatedResponses) {

            if (considerTargetOnly) {
                if (StringUtils.isNotEmpty(relation.getTarget())
                        && StringUtils.isNotEmpty(sourceRelation.getTarget())
                        && relation.getTarget().equalsIgnoreCase(sourceRelation.getTarget())) {
                    return true;
                }
            } else {
                if (StringUtils.isNotEmpty(relation.getSource())
                        && StringUtils.isNotEmpty(sourceRelation.getSource())
                        && StringUtils.isNotEmpty(relation.getTarget())
                        && StringUtils.isNotEmpty(sourceRelation.getTarget())
                        && relation.getSource().equalsIgnoreCase(sourceRelation.getSource())
                        && relation.getTarget().equalsIgnoreCase(sourceRelation.getTarget())) {

                    return true;
                }
            }
        }

        return false;
    }

    /**
     *
     * @param errorClassName
     * @return
     */
    private ClassRelation getErrorClass(String errorClassName) {
        ClassRelation classRelation = new ClassRelation();
        classRelation.setTarget(errorClassName);
        classRelation.setComposition(false);
        classRelation.setExtension(true);

        return classRelation;
    }

    /**
     *
     * @param operation
     * @return
     */
    private List<ClassRelation> getInterfaceRelatedInputs(Operation operation) {
        List<ClassRelation> relatedResponses = new ArrayList<ClassRelation>();
        List<Parameter> parameters = operation.getParameters();

        for (Parameter parameter : parameters) {
            if (parameter instanceof BodyParameter) {
                Model bodyParameter = ((BodyParameter) parameter).getSchema();

                if (bodyParameter instanceof RefModel) {

                    ClassRelation classRelation = new ClassRelation();
                    classRelation.setTarget(((RefModel) bodyParameter).getSimpleRef());
                    classRelation.setComposition(false);
                    classRelation.setExtension(true);

                    relatedResponses.add(classRelation);
                } else if (bodyParameter instanceof ArrayModel) {
                    Property propertyObject = ((ArrayModel) bodyParameter).getItems();

                    if (propertyObject instanceof RefProperty) {
                        ClassRelation classRelation = new ClassRelation();
                        classRelation.setTarget(((RefProperty) propertyObject).getSimpleRef());
                        classRelation.setComposition(false);
                        classRelation.setExtension(true);

                        relatedResponses.add(classRelation);
                    }
                }
            }
        }

        return relatedResponses;
    }

    /**
     *
     * @param operation
     * @return
     */
    private List<ClassRelation> getInterfaceRelatedResponses(Operation operation) {
        List<ClassRelation> relatedResponses = new ArrayList<ClassRelation>();
        Map<String, Response> responses = operation.getResponses();

        for (Map.Entry<String, Response> responsesEntry : responses.entrySet()) {
            String responseCode = responsesEntry.getKey();

            if (!(responseCode.equalsIgnoreCase("default") || Integer.parseInt(responseCode) >= 300)) {
                Property responseProperty = responsesEntry.getValue().getSchema();

                if (responseProperty instanceof RefProperty) {
                    ClassRelation relation = new ClassRelation();
                    relation.setTarget(((RefProperty) responseProperty).getSimpleRef());
                    relation.setComposition(false);
                    relation.setExtension(true);

                    relatedResponses.add(relation);
                } else if (responseProperty instanceof ArrayProperty) {
                    ArrayProperty arrayObject = (ArrayProperty) responseProperty;
                    Property arrayResponseProperty = arrayObject.getItems();

                    if (arrayResponseProperty instanceof RefProperty) {
                        ClassRelation relation = new ClassRelation();
                        relation.setTarget(((RefProperty) arrayResponseProperty).getSimpleRef());
                        relation.setComposition(false);
                        relation.setExtension(true);

                        relatedResponses.add(relation);
                    }
                }
            }

        }

        return relatedResponses;
    }

    /**
     *
     * @param operation
     * @return
     */
    private List<MethodDefinitions> getInterfaceMethods(Operation operation) {
        List<MethodDefinitions> interfaceMethods = new ArrayList<MethodDefinitions>();
        MethodDefinitions methodDefinitions = new MethodDefinitions();
        methodDefinitions.setMethodDefinition(new StringBuilder().append(operation.getOperationId()).append("(")
                .append(getMethodParameters(operation)).append(")").toString());
        methodDefinitions.setReturnType(getInterfaceReturnType(operation));

        interfaceMethods.add(methodDefinitions);

        return interfaceMethods;
    }

    /**
     *
     * @param operation
     * @return
     */
    private String getMethodParameters(Operation operation) {
        String methodParameter = "";
        List<Parameter> parameters = operation.getParameters();

        for (Parameter parameter : parameters) {
            if (StringUtils.isNotEmpty(methodParameter)) {
                methodParameter = new StringBuilder().append(methodParameter).append(",").toString();
            }

            if (parameter instanceof PathParameter) {
                methodParameter = new StringBuilder().append(methodParameter)
                        .append(toTitleCase(((PathParameter) parameter).getType())).append(" ")
                        .append(((PathParameter) parameter).getName()).toString();
            } else if (parameter instanceof QueryParameter) {
                Property queryParameterProperty = ((QueryParameter) parameter).getItems();

                if (queryParameterProperty instanceof RefProperty) {
                    methodParameter = new StringBuilder().append(methodParameter)
                            .append(toTitleCase(((RefProperty) queryParameterProperty).getSimpleRef())).append("[] ")
                            .append(((BodyParameter) parameter).getName()).toString();
                } else if (queryParameterProperty instanceof StringProperty) {
                    methodParameter = new StringBuilder().append(methodParameter)
                            .append(toTitleCase(((StringProperty) queryParameterProperty).getType())).append("[] ")
                            .append(((QueryParameter) parameter).getName()).toString();
                } else {
                    methodParameter = new StringBuilder().append(methodParameter)
                            .append(toTitleCase(((QueryParameter) parameter).getType())).append(" ")
                            .append(((QueryParameter) parameter).getName()).toString();
                }
            } else if (parameter instanceof BodyParameter) {
                Model bodyParameter = ((BodyParameter) parameter).getSchema();

                if (bodyParameter instanceof RefModel) {
                    methodParameter = new StringBuilder().append(methodParameter)
                            .append(toTitleCase(((RefModel) bodyParameter).getSimpleRef())).append(" ")
                            .append(((BodyParameter) parameter).getName()).toString();
                } else if (bodyParameter instanceof ArrayModel) {
                    Property propertyObject = ((ArrayModel) bodyParameter).getItems();

                    if (propertyObject instanceof RefProperty) {
                        methodParameter = new StringBuilder().append(methodParameter)
                                .append(toTitleCase(((RefProperty) propertyObject).getSimpleRef())).append("[] ")
                                .append(((BodyParameter) parameter).getName()).toString();
                    }
                }
            } else if (parameter instanceof FormParameter) {
                methodParameter = new StringBuilder().append(methodParameter)
                        .append(toTitleCase(((FormParameter) parameter).getType())).append(" ")
                        .append(((FormParameter) parameter).getName()).toString();
            }
        }

        return methodParameter;
    }

    /**
     *
     * @param operation
     * @return
     */
    private String getInterfaceReturnType(Operation operation) {
        String returnType = "void";

        Map<String, Response> responses = operation.getResponses();
        for (Map.Entry<String, Response> responsesEntry : responses.entrySet()) {
            String responseCode = responsesEntry.getKey();

            if (!(responseCode.equalsIgnoreCase("default") || Integer.parseInt(responseCode) >= 300)) {
                Property responseProperty = responsesEntry.getValue().getSchema();

                if (responseProperty instanceof RefProperty) {
                    returnType = ((RefProperty) responseProperty).getSimpleRef();
                } else if (responseProperty instanceof ArrayProperty) {
                    Property arrayResponseProperty = ((ArrayProperty) responseProperty).getItems();
                    if (arrayResponseProperty instanceof RefProperty) {
                        returnType = new StringBuilder().append(((RefProperty) arrayResponseProperty).getSimpleRef())
                                .append("[]").toString();
                    }
                } else if (responseProperty instanceof ObjectProperty) {
                    returnType = new StringBuilder().append(toTitleCase(operation.getOperationId())).append("Generated")
                            .toString();
                }
            }
        }

        return returnType;
    }

    /**
     *
     * @param operation
     * @return
     */
    private String getErrorClassName(Operation operation) {
        StringBuilder errorClass = new StringBuilder();
        Map<String, Response> responses = operation.getResponses();
        for (Map.Entry<String, Response> responsesEntry : responses.entrySet()) {
            String responseCode = responsesEntry.getKey();

            if (responseCode.equalsIgnoreCase("default") || Integer.parseInt(responseCode) >= 300) {
                Property responseProperty = responsesEntry.getValue().getSchema();

                if (responseProperty instanceof RefProperty) {
                    String errorClassName = ((RefProperty) responseProperty).getSimpleRef();
                    if (!errorClass.toString().contains(errorClassName)) {
                        if (StringUtils.isNotEmpty(errorClass)) {
                            errorClass.append(",");
                        }
                        errorClass.append(errorClassName);
                    }
                }
            }
        }

        return errorClass.toString();
    }

    /**
     *
     * @param tags
     * @param operation
     * @param uri
     * @return
     */
    private String getInterfaceName(List<String> tags, Operation operation, String uri) {
        String interfaceName;

        if (!tags.isEmpty()) {
            interfaceName = toTitleCase(tags.get(0).replaceAll(" ", ""));
        } else if (StringUtils.isNotEmpty(operation.getOperationId())) {
            interfaceName = toTitleCase(operation.getOperationId());
        } else {
            interfaceName = toTitleCase(uri.replaceAll("{", "").replaceAll("}", "").replaceAll("\\", ""));
        }

        return new StringBuilder().append(interfaceName).append("Api").toString();
    }

    /**
     *
     * @param model
     * @return
     */
    private String getSuperClass(Model model) {
        LOGGER.entering(LOGGER.getName(), "getSuperClass");

        String superClass = null;

        if (model instanceof ArrayModel) {
            ArrayModel arrayModel = (ArrayModel) model;
            Property propertyObject = arrayModel.getItems();

            if (propertyObject instanceof RefProperty) {
                superClass = new StringBuilder().append("ArrayList[")
                        .append(((RefProperty) propertyObject).getSimpleRef()).append("]").toString();
            }
        } else if (model instanceof ModelImpl) {
            Property addProperty = ((ModelImpl) model).getAdditionalProperties();

            if (addProperty instanceof RefProperty) {
                superClass = new StringBuilder().append("Map[").append(((RefProperty) addProperty).getSimpleRef())
                        .append("]").toString();
            }
        }

        LOGGER.exiting(LOGGER.getName(), "getSuperClass");

        return superClass;
    }

    /**
     *
     * @param className
     * @param isArray
     * @return
     */
    protected String getDataType(String className, boolean isArray) {
        if (isArray) {
            return new StringBuilder().append(toTitleCase(className)).append("[]").toString();
        }

        return toTitleCase(className);
    }

    /**
     *
     * @param input
     * @return
     */
    private String toTitleCase(String input) {
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }

            titleCase.append(c);
        }

        return titleCase.toString();
    }

    /**
     *
     * @param model
     * @return
     */
    private boolean isModelClass(Model model) {
        LOGGER.entering(LOGGER.getName(), "isModelClass");

        boolean isModelClass = true;

        if (model instanceof ModelImpl) {
            List<String> enumValues = ((ModelImpl) model).getEnum();

            if (enumValues != null && !enumValues.isEmpty()) {
                isModelClass = false;
            }
        }

        LOGGER.exiting(LOGGER.getName(), "isModelClass");

        return isModelClass;
    }

}