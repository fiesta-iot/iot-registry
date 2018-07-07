/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.storage.sql;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.fiesta_iot.platform.iot_registry.config.IoTRegistryConfiguration;

@Entity
@Table(name = "sparql_query")
@NamedQueries({
        @NamedQuery(name = "Query.findAllIds",
                    query = "SELECT q.id FROM SparqlQuery q"),
        @NamedQuery(name = "Query.findIdsByScope",
                    query = "SELECT q.id FROM SparqlQuery q WHERE q.scope = :scope"), 
})
@XmlRootElement(name = "query")
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class SparqlQuery implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final String SPARQL_TEMPLATE_VAR_DELIMITER =
	        IoTRegistryConfiguration.getInstance()
	                .getSparqlTemplateVarDelimiter();

	// To avoid problems with case sensitiveness in the value
	public enum Scope {
		GLOBAL,
		RESOURCES,
		OBSERVATIONS;

		public static Scope fromString(String param) {
			String toUpper = param.toUpperCase();
			return valueOf(toUpper);
		}

		// private static Map<String, Scope> namesMap = new HashMap<String,
		// Scope>(3);
		//
		// static {
		// namesMap.put("GLOBAL", GLOBAL);
		// namesMap.put("RESOURCE", RESOURCE);
		// namesMap.put("OBSERVATION", OBSERVATION);
		// }
		//
		// @JsonCreator
		// public static Scope forValue(String value) {
		// System.out.println("dentro Forvalue " + value);
		// return namesMap.get(value.toUpperCase());
		// }
		//
		// @XmlValue
		// @JsonValue
		// public String toValue() {
		// System.out.println("dentro de value");
		// for (Entry<String, Scope> entry : namesMap.entrySet()) {
		// if (entry.getValue() == this)
		// return entry.getKey();
		// }
		//
		// return null; // or fail
		// }
	}

	private long id;

	private String value;

	private String name;

	private String description;

	private Scope scope;

	private OffsetDateTime created;

	private OffsetDateTime modified;

	private List<String> vars;

	public SparqlQuery() {
	}

	public SparqlQuery(String name, String value, String description) {
		setName(name);
		setValue(value);
		setDescription(description);
	}

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@XmlElement(name = "id")
	@JsonProperty("id")
	@JsonInclude(Include.NON_NULL)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@NotNull
	@Column(name = "value")
	@XmlElement(name = "value")
	@JsonProperty("value")
	@JsonInclude(Include.NON_NULL)
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
		setVars(retrieveVariablesFromTemplate());
	}

	@NotNull
	@Column(name = "name")
	@XmlElement(name = "name")
	@JsonProperty("name")
	@JsonInclude(Include.NON_NULL)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "description")
	@XmlElement(name = "description")
	@JsonProperty("description")
	@JsonInclude(Include.NON_NULL)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@NotNull
	@Column(name = "scope")
	@XmlElement(name = "scope")
	@Enumerated(EnumType.STRING)
	@XmlJavaTypeAdapter(JaxBCaseInsensitiveEnumAdapter.class)
	@JsonProperty("scope")
	@JsonInclude(Include.NON_NULL)
	public Scope getScope() {
		return scope;
	}

	public void setScope(Scope scope) {
		this.scope = scope;
	}

	@NotNull
	@Column(name = "created",
	        columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
//	@Temporal(TemporalType.TIMESTAMP)
	@XmlElement(name = "created")
//	@XmlJavaTypeAdapter(JaxBOffsetDateTimeAdapter.class)
	@XmlJavaTypeAdapter(JaxBOffsetDateTimeAdapter.class)
	@JsonProperty("created")
	@JsonInclude(Include.NON_NULL)
	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(OffsetDateTime created) {
		this.created = created;
	}

	@NotNull
	@Column(name = "modified",
	        columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
//	@Temporal(TemporalType.TIMESTAMP)
	@XmlElement(name = "modified")
	@XmlSchemaType(name = "dateTimeStamp")
	@XmlJavaTypeAdapter(JaxBOffsetDateTimeAdapter.class)
	@JsonProperty("modified")
	@JsonInclude(Include.NON_NULL)
//	@JsonSerialize(using = OffsetDateTimeSerializer.class)
//	@JsonDeserialize(using = OffsetDateTimeDeserializer.class)
	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(OffsetDateTime modified) {
		this.modified = modified;
	}

	@Transient
	@XmlElement(name = "var")
	// @XmlList
	@XmlElementWrapper(name = "vars")
	@JsonProperty("vars")
	@JsonInclude(Include.NON_NULL)
	public List<String> getVars() {
		return vars;
	}

	private void setVars(List<String> params) {
		this.vars = params;
	}

	@Transient
	@XmlTransient
	@JsonIgnore
	// Test Sparql sentence
	public boolean isValidSparqlFormat() {
		// Replace all SPARQL_TEMPLATE_VAR_DELIMITER, so the variable becomes a
		// string
		String q =
		        this.getValue().replaceAll(SPARQL_TEMPLATE_VAR_DELIMITER, "'");
		try {
			QueryFactory.create(q);
			return true;
		} catch (QueryException ex) {
			return false;
		}
	}

	private List<String> retrieveVariablesFromTemplate() {
		return retrieveVariablesFromTemplate(this.getValue());
	}

	private List<String> retrieveVariablesFromTemplate(String queryTemplate) {
		// Only valid for Subject and Object. Currently not running on Predicate
		LinkedHashSet<String> queryParams = new LinkedHashSet<>();
		String SPARQL_TEMPLATE_VAR_DELIMITER = "%%%";
		Pattern p = Pattern.compile(SPARQL_TEMPLATE_VAR_DELIMITER + "(.*?)"
		                            + SPARQL_TEMPLATE_VAR_DELIMITER);
		Matcher m = p.matcher(queryTemplate);
		while (m.find()) {
			queryParams.add(m.group(1));
		}

		return new ArrayList<String>(queryParams);
	}

	private String replaceTemplateVariable(String queryTemplate, String name,
	                                       String value) {
		return queryTemplate.replaceAll(SPARQL_TEMPLATE_VAR_DELIMITER + name
		                                + SPARQL_TEMPLATE_VAR_DELIMITER, value);
	}

	// For the moment is case sensitive
	public List<String>
	        replaceTemplateVariables(MultivaluedMap<String, String> vars) {
		List<String> templateVars = getVars();
		List<String> remainingVars = new ArrayList<>();

		String queryTemplate = this.getValue();
		for (String var : templateVars) {
			String value = vars.getFirst(var);
			if (value != null) {
				queryTemplate =
				        replaceTemplateVariable(queryTemplate, var, value);
			} else {
				remainingVars.add(var);
			}
		}

		this.setValue(queryTemplate);

		return remainingVars;
	}
}
