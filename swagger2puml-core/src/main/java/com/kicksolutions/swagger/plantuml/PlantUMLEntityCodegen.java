package com.kicksolutions.swagger.plantuml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.kicksolutions.swagger.plantuml.vo.EntityDiagram;
import com.kicksolutions.swagger.plantuml.vo.EntityField;
import com.kicksolutions.swagger.plantuml.vo.EntityRelation;

import io.swagger.models.Model;
import io.swagger.models.Swagger;

/**
 * @author MSANTOSH
 * @author David Gileadi
 *
 */
public class PlantUMLEntityCodegen extends PlantUMLCodegen<EntityDiagram, EntityField, EntityRelation> {

    public PlantUMLEntityCodegen(Swagger swagger, File targetLocation) {
        super(swagger, targetLocation);
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

        List<EntityDiagram> diagrams = processSwaggerModels(swagger);
        additionalProperties.put("entityDiagrams", diagrams);

        additionalProperties.put("entityRelations", getRelations(diagrams));

        LOGGER.exiting(LOGGER.getName(), "preprocessSwagger");

        return additionalProperties;
    }

    @Override
    protected EntityDiagram createDiagram(String name, Model model, List<EntityField> members,
            List<EntityRelation> relations) {

        Map<Boolean, List<EntityField>> groups = members.stream().collect(Collectors.partitioningBy(f -> isKeyField(f)));
        List<EntityField> keyFields = groups.get(true);
        List<EntityField> fields = groups.get(false);;

        return new EntityDiagram(name, model.getDescription(), keyFields, fields, relations);
    }

    @Override
    protected EntityField createMember(String name, String dataType, String entityName, Model model) {

        boolean isRequired = isRequiredProperty(model, name);
        return new EntityField(dataType, name, entityName, isRequired);
    }

    @Override
    protected boolean addRelation(EntityField member, Model model, List<EntityRelation> addTo) {

        boolean isMany = member.getDataType() != null && member.getDataType().endsWith("[]");

        EntityRelation relation = new EntityRelation(member.getEntityName(), member.isRequired(), isMany, null,
                member.getName());

        addTo.add(relation);
        return true;
    }

    /**
     *
     * @param diagrams
     * @return
     */
    private List<EntityRelation> getRelations(List<EntityDiagram> diagrams) {
        List<EntityRelation> modelRelations = new ArrayList<EntityRelation>();

        for (EntityDiagram diagram : diagrams) {
            List<EntityRelation> entityRelations = diagram.getRelations();

            for (EntityRelation relation : entityRelations) {
                relation.setSource(diagram.getName());
                modelRelations.add(relation);
            }
        }

        return modelRelations;
    }

    private boolean isKeyField(EntityField field) {
        String name = field.getName();
        return "id".equalsIgnoreCase(name) || "_id".equalsIgnoreCase(name)
                || "key".equalsIgnoreCase(name)
                || (field.getEntityName() + "id").equalsIgnoreCase(name)
                || (field.getEntityName() + "_id").equalsIgnoreCase(name)
                || (field.getEntityName() + "key").equalsIgnoreCase(name)
                || (field.getEntityName() + "_key").equalsIgnoreCase(name);
    }

}