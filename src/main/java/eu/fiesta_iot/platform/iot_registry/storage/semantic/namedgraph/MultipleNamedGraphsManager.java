/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
 package eu.fiesta_iot.platform.iot_registry.storage.semantic.namedgraph;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.query.Dataset;
import org.apache.jena.system.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.config.IoTRegistryConfiguration;

public class MultipleNamedGraphsManager extends NamedGraphsManager {

	private static Logger log =
	        LoggerFactory.getLogger(MultipleNamedGraphsManager.class);

	private static final String SEPARATOR = "-";

	// TODO: Analyse if it is required to set the seconds or not
	private static final DateTimeFormatter dateTimeFormatter =
	        new DateTimeFormatterBuilder().parseCaseInsensitive()
	                .appendValue(ChronoField.YEAR, 4).appendPattern("MMddHHmm")
	                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 59)
	                .toFormatter();

	private static long backupPeriod =
	        IoTRegistryConfiguration.getInstance().getBackupPeriod() * 60;

	private static List<LocalDateTime> graphDates = null;
	private static String currentName = null;

	public MultipleNamedGraphsManager(String baseName, Dataset dataset) {
		super(baseName + SEPARATOR, dataset);
		initializeGraphDates();
	}

	protected boolean isInitialized() {
		return (currentName != null);
	}

	private synchronized void initializeGraphDates() {
		if (!isInitialized()) {
			graphDates = listAvailableGraphDates();
			if (graphDates.isEmpty()) {
				setCurrentName();
			} else {
				setCurrentName(generateName(graphDates.get(0)));
			}
		}
	}

	public List<String> listAvailableGraphNames() {
		return _listAvailableGraphs().collect(Collectors.toList());
	}

	public List<LocalDateTime> listAvailableGraphDates() {
		return _listAvailableGraphs().map(name -> getDate(name))
		        .collect(Collectors.toList());
	}

	private Stream<String> _listAvailableGraphs() {
		return Txn.calculateRead(dataset, () -> {
			return Iter.asStream(dataset.listNames(), true)
			        //.peek(num -> System.out.println("will filter " + num))
			        .filter(name -> isValidName(name))
			        .sorted(Comparator.reverseOrder());
		});
	}

	@Override
	public List<String> listAvailableGraphs() {
		return graphDates.stream().map(date -> generateName(date))
		        .collect(Collectors.toList());
	}

	private String generateName() {
		return generateName(now());
	}

	private static LocalDateTime now() {
		return LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
		// return LocalDateTime.now(ZoneId.of("UTC"));
	}

	private String generateName(LocalDateTime date) {
		return baseName + date.format(dateTimeFormatter);
	}

	protected boolean isValidName(String name) {
		if (name != null) {
			return name.startsWith(baseName);
		}
		return false;
	}

	private LocalDateTime getDate(String name) {
		// Current graph have the most updated date
		// if (name.equals(ObservationsNamedGraphsManager.currentName)) {
		// return now();
		// }
		if (!isValidName(name)) {
			throw new IllegalArgumentException(name
			                                   + "is not a valid graph name");
		}

		// Extract date
		return LocalDateTime.parse(name.substring(baseName.length()),
		                           dateTimeFormatter);
	}

	public static synchronized void setBackupPeriod(int backupPeriod) {
		MultipleNamedGraphsManager.backupPeriod = backupPeriod;
	}

	@Override
	protected String getReadCurrentName() {
		return currentName;
	}

	@Override
	protected String getWriteCurrentName() {
		LocalDateTime currentTime = getDate(currentName);
		// LocalDateTime currentTime = graphDates.get(0);
		LocalDateTime now = now();
		if (now.isAfter(currentTime.plusSeconds(backupPeriod))) {
			return generateName(now);
		} else {
			return currentName;
		}
	}

	public void setCurrentName() {
		setCurrentName(generateName());
	}

	public void setCurrentName(String name) {
		if (isValidName(name)) {
			currentName = name;
		}
		log.debug("Current observations graph name: " + currentName);
	}

	@Override
	protected void addGraphName(String name) {
		setCurrentName(name);
		graphDates.add(0, getDate(name));
	}

	@Override
	protected boolean containsGraphName(String name) {
		return graphDates.contains(getDate(name));
	}

	@Override
	public void resetCurrentName() {
		setCurrentName();
	}

	protected static boolean withinDatesInterval(LocalDateTime dateTime,
	                                             LocalDateTime startDate,
	                                             LocalDateTime endDate) {
		boolean result = true;
		if (startDate != null) {
			result &= !dateTime.isBefore(startDate);
		}

		if (endDate != null) {
			result &= !dateTime.isAfter(endDate);
		}

		return result;
	}

	@Override
	protected List<String> getIntervalModel(LocalDateTime startDate,
	                                        LocalDateTime endDate) {
		// In order to avoid iterate
		if (startDate == null && endDate == null) {
			return new ArrayList<String>(Arrays.asList(getReadCurrentName()));
			//return Arrays.asList(getReadCurrentName());
		}

		return graphDates.stream()
		        // .peek(num -> log.debug("will filter " + num))
		        .filter(date -> withinDatesInterval(date, startDate, endDate))
		        // .peek(num -> log.debug("Graph included " + num))
		        .map(date -> generateName(date)).collect(Collectors.toList());
	}
}
