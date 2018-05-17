package com.kicksolutions.swagger.plantuml.vo;

import java.util.List;

/**
 *
 * @author David Gileadi
 *
 */
public class EntityDiagram {

	private String name;
	private String description;
	private List<EntityField> keyFields;
	private List<EntityField> fields;
	private List<EntityRelation> relations;

	public EntityDiagram(String name, String description, List<EntityField> keyFields, List<EntityField> fields,
			List<EntityRelation> relations) {
		super();
		this.name = name;
		this.description = description;
		this.keyFields = keyFields;
		this.fields = fields;
		this.relations = relations;
	}

	public EntityDiagram(){
		super();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<EntityField> getKeyFields() {
		return keyFields;
	}

	public void setKeyFields(List<EntityField> keyFields) {
		this.keyFields = keyFields;
	}

	public List<EntityField> getFields() {
		return fields;
	}

	public void setFields(List<EntityField> fields) {
		this.fields = fields;
	}

	public List<EntityRelation> getRelations() {
		return relations;
	}

	public void setRelations(List<EntityRelation> relations) {
		this.relations = relations;
	}

	@Override
	public String toString() {
		return "ClassDiagram [name=" + name + ", description=" + description
				+ ", keyFields=" + keyFields+ ", fields=" + fields + ", relations=" + relations + "]";
	}
}