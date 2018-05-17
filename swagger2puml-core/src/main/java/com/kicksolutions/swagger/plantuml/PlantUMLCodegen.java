/**
 *
 */
package com.kicksolutions.swagger.plantuml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.kicksolutions.swagger.plantuml.vo.Member;
import com.kicksolutions.swagger.plantuml.vo.Relation;

import io.swagger.models.ArrayModel;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;

/**
 * @author MSANTOSH
 *
 */
public abstract class PlantUMLCodegen<D, M extends Member, R extends Relation> {

	protected static final Logger LOGGER = Logger.getLogger(PlantUMLCodegen.class.getName());

	private final Swagger swagger;
	private final File targetLocation;

	/**
	 * Preprocess the given Swagger, creating a property map for the Mustache
	 * template.
	 */
	protected abstract Map<String, Object> preprocessSwagger(Swagger swagger);

	/**
	 * Create a diagram of the correct type.
	 */
	protected abstract D createDiagram(String className, Model model, List<M> members, List<R> relations);

	/**
	 * Create a member of the correct type for the given model.
	 */
	protected abstract M createMember(String name, String dataType, String className, Model model);

	/**
	 * Create a relation of the correct type between the member and the model,
	 * and add it to the given list.
	 */
	protected abstract boolean addRelation(M member, Model model, List<R> addTo);

	/**
	 *
	 */
	public PlantUMLCodegen(Swagger swagger, File targetLocation) {
		this.swagger = swagger;
		this.targetLocation = targetLocation;
	}

	/**
	 *
	 */
	public String generatePuml() throws IOException, IllegalAccessException {
		LOGGER.entering(LOGGER.getName(), "generatePuml");

		Map<String, Object> additionalProperties = preprocessSwagger(swagger);

		MustacheFactory mf = new DefaultMustacheFactory();
		Mustache mustache = mf.compile("puml.mustache");
		Writer writer = null;
		String pumlPath = new StringBuilder().append(targetLocation.getAbsolutePath()).append(File.separator)
				.append("swagger.puml").toString();
		try {
			writer = new FileWriter(pumlPath);
			mustache.execute(writer, additionalProperties);

			LOGGER.log(Level.FINEST, "Sucessfully Written Puml File @ " + pumlPath);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			throw new IllegalAccessException(e.getMessage());
		} finally {
			if (writer != null) {
				writer.flush();
			}
		}

		LOGGER.exiting(LOGGER.getName(), "generatePuml");
		return pumlPath;
	}

	/**
	 *
	 * @param swagger
	 * @return
	 */
	public List<D> processSwaggerModels(Swagger swagger) {
		LOGGER.entering(LOGGER.getName(), "processSwaggerModels");

		List<D> diagrams = new ArrayList<D>();
		Map<String, Model> modelsMap = swagger.getDefinitions();

		for (Map.Entry<String, Model> models : modelsMap.entrySet()) {
			String className = models.getKey();
			Model modelObject = models.getValue();

			LOGGER.info("Processing Model " + className);

			List<M> members = getMembers(modelObject, modelsMap);
			List<R> relations = getRelations(members, modelObject);

			diagrams.add(createDiagram(className, modelObject, members, relations));
		}

		LOGGER.exiting(LOGGER.getName(), "processSwaggerModels");

		return diagrams;
	}

	/**
	 *
	 * @param members
	 * @param modelObject
	 * @return
	 */
	private List<R> getRelations(List<M> members, Model modelObject) {
		LOGGER.entering(LOGGER.getName(), "getRelations");

		List<R> relations = new ArrayList<R>();

		for (M member : members) {
			if (member.getClassName() != null && member.getClassName().trim().length() > 0) {
				addRelation(member, modelObject, relations);
			}
		}

		LOGGER.exiting(LOGGER.getName(), "getRelations");

		return relations;
	}

	/**
	 *
	 * @param modelObject
	 * @param modelsMap
	 * @return
	 */
	private List<M> getMembers(Model modelObject, Map<String, Model> modelsMap) {
		LOGGER.entering(LOGGER.getName(), "getMembers");

		List<M> members = new ArrayList<M>();

		if (modelObject instanceof ModelImpl) {
			members = getMembers((ModelImpl) modelObject, modelsMap);
		} else if (modelObject instanceof ComposedModel) {
			members = getMembers((ComposedModel) modelObject, modelsMap);
		} else if (modelObject instanceof ArrayModel) {
			members = getMembers((ArrayModel) modelObject, modelsMap);
		}

		LOGGER.exiting(LOGGER.getName(), "getMembers");
		return members;
	}

	/**
	 *
	 * @param arrayModel
	 * @param modelsMap
	 * @return
	 */
	private List<M> getMembers(ArrayModel arrayModel, Map<String, Model> modelsMap) {
		LOGGER.entering(LOGGER.getName(), "getMembers-ArrayModel");

		List<M> members = new ArrayList<M>();

		Property propertyObject = arrayModel.getItems();

		if (propertyObject instanceof RefProperty) {
			members.add(getRefMembers((RefProperty) propertyObject, arrayModel));
		}

		LOGGER.exiting(LOGGER.getName(), "getMembers-ArrayModel");
		return members;
	}

	/**
	 *
	 * @param composedModel
	 * @param modelsMap
	 * @return
	 */
	private List<M> getMembers(ComposedModel composedModel, Map<String, Model> modelsMap) {
		LOGGER.entering(LOGGER.getName(), "getMembers-ComposedModel");

		List<M> members = new ArrayList<M>();

		Map<String, Property> childProperties = new HashMap<String, Property>();

		if (null != composedModel.getChild()) {
			childProperties = composedModel.getChild().getProperties();
		}

		List<Model> allOf = composedModel.getAllOf();
		for (Model currentModel : allOf) {

			if (currentModel instanceof RefModel) {
				RefModel refModel = (RefModel) currentModel;
				childProperties.putAll(modelsMap.get(refModel.getSimpleRef()).getProperties());

				members = convertModelPropertiesToMembers(childProperties, modelsMap.get(refModel.getSimpleRef()),
						modelsMap);
			}
		}

		LOGGER.exiting(LOGGER.getName(), "getMembers-ComposedModel");
		return members;
	}

	/**
	 *
	 * @param model
	 * @return
	 */
	private List<M> getMembers(ModelImpl model, Map<String, Model> modelsMap) {
		LOGGER.entering(LOGGER.getName(), "getMembers-ModelImpl");

		List<M> members = new ArrayList<M>();

		Map<String, Property> modelMembers = model.getProperties();
		if (modelMembers != null && !modelMembers.isEmpty()) {
			members.addAll(convertModelPropertiesToMembers(modelMembers, model, modelsMap));
		} else {
			Property modelAdditionalProps = model.getAdditionalProperties();

			if (modelAdditionalProps instanceof RefProperty) {
				members.add(getRefMembers((RefProperty) modelAdditionalProps, model));
			}

			if (modelAdditionalProps == null) {
				List<String> enumValues = model.getEnum();

				if (enumValues != null && !enumValues.isEmpty()) {
					members.addAll(getEnum(enumValues));
				}
			}
		}

		LOGGER.exiting(LOGGER.getName(), "getMembers-ModelImpl");

		return members;
	}

	/**
	 *
	 * @param refProperty
	 * @return
	 */
	private M getRefMembers(RefProperty refProperty, Model model) {
		LOGGER.entering(LOGGER.getName(), "getRefMembers");
		M classMember = createMember(" ", null, refProperty.getSimpleRef(), model);

		LOGGER.exiting(LOGGER.getName(), "getRefMembers");
		return classMember;
	}

	/**
	 *
	 * @param enumValues
	 * @return
	 */
	private List<M> getEnum(List<String> enumValues) {
		LOGGER.entering(LOGGER.getName(), "getEnum");

		List<M> members = new ArrayList<M>();

		if (enumValues != null && !enumValues.isEmpty()) {
			for (String enumValue : enumValues) {
				M classMember = createMember(enumValue, null, null, null);
				members.add(classMember);
			}
		}

		LOGGER.exiting(LOGGER.getName(), "getEnum");
		return members;
	}

	/**
	 *
	 * @param modelMembers
	 * @return
	 */
	private List<M> convertModelPropertiesToMembers(Map<String, Property> modelMembers, Model modelObject,
			Map<String, Model> models) {
		LOGGER.entering(LOGGER.getName(), "convertModelPropertiesToMembers");

		List<M> members = new ArrayList<M>();

		for (Map.Entry<String, Property> modelMapObject : modelMembers.entrySet()) {
			String variableName = modelMapObject.getKey();

			final M member;
			Property property = modelMembers.get(variableName);

			if (property instanceof ArrayProperty) {
				member = getMember((ArrayProperty) property, modelObject, models, variableName);
			} else if (property instanceof RefProperty) {
				member = getMember((RefProperty) property, models, modelObject, variableName, false);
			} else {
				String dataType = getDataType(property.getFormat() != null ? property.getFormat() : property.getType(),
						false);
				member = createMember(variableName, dataType, null, null);
			}

			members.add(member);
		}

		LOGGER.exiting(LOGGER.getName(), "convertModelPropertiesToMembers");
		return members;
	}

	/**
	 *
	 * @param modelObject
	 * @param models
	 * @param variableName
	 * @param member
	 * @param propObject
	 */
	private M getMember(ArrayProperty property, Model modelObject, Map<String, Model> models, String variableName) {
		LOGGER.entering(LOGGER.getName(), "getMember-ArrayProperty");

		final M member;
		Property propObject = property.getItems();

		if (propObject instanceof RefProperty) {
			member = getMember((RefProperty) propObject, models, modelObject, variableName, true);
		} else if (propObject instanceof StringProperty) {
			member = getMember((StringProperty) propObject, variableName);
		} else {
			throw new RuntimeException("Unsupported property type " + propObject);
		}

		LOGGER.exiting(LOGGER.getName(), "getMember-ArrayProperty");
		return member;
	}

	/**
	 *
	 * @param stringProperty
	 * @param models
	 * @param modelObject
	 * @param variableName
	 * @return
	 */
	private M getMember(StringProperty stringProperty, String variableName) {
		LOGGER.entering(LOGGER.getName(), "getMember-StringProperty");

		M member = createMember(variableName, getDataType(stringProperty.getType(), true), null, null);

		LOGGER.exiting(LOGGER.getName(), "getMember-StringProperty");
		return member;
	}

	/**
	 *
	 * @param refProperty
	 * @param models
	 * @param modelObject
	 * @param variableName
	 * @return
	 */
	private M getMember(RefProperty refProperty, Map<String, Model> models, Model modelObject, String variableName, boolean isArray) {
		LOGGER.entering(LOGGER.getName(), "getMember-RefProperty");

		String dataType = getDataType(refProperty.getSimpleRef(), isArray);
		String className = models.containsKey(refProperty.getSimpleRef()) ? refProperty.getSimpleRef() : null;

		M member = createMember(variableName, dataType, className, modelObject);

		LOGGER.exiting(LOGGER.getName(), "getMember-RefProperty");
		return member;
	}

	/**
	 *
	 * @param className
	 * @param isArray
	 * @return
	 */
	protected String getDataType(String className, boolean isArray) {
		if (isArray) {
			return new StringBuilder().append(className).append("[]").toString();
		}

		return className;
	}

	/**
	 *
	 * @param modelObject
	 * @param propertyName
	 * @return
	 */
	protected boolean isRequiredProperty(Model modelObject, String propertyName) {
		boolean isRequiredProperty = false;
		LOGGER.entering(LOGGER.getName(), "isRequiredProperty");

		if (modelObject != null) {
			if (modelObject instanceof ModelImpl) {
				List<String> requiredProperties = ((ModelImpl) modelObject).getRequired();
				if (requiredProperties != null && !requiredProperties.isEmpty()) {
					isRequiredProperty = requiredProperties.contains(propertyName);
				}
			} else {
				isRequiredProperty = false;
			}
		}

		LOGGER.exiting(LOGGER.getName(), "isRequiredProperty");
		return isRequiredProperty;
	}

}