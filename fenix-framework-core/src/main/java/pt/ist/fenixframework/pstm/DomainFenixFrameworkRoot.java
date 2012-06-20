package pt.ist.fenixframework.pstm;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import pt.ist.fenixframework.Config;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.pstm.consistencyPredicates.ConsistencyPredicate;
import pt.ist.fenixframework.pstm.consistencyPredicates.DomainConsistencyPredicate;
import pt.ist.fenixframework.pstm.consistencyPredicates.DomainDependenceRecord;
import dml.DomainClass;
import dml.DomainModel;

/**
 * The <code>DomainFenixFrameworkRoot</code> is a singleton root object that is
 * related to all the {@link DomainMetaClass}es in the system.<br>
 * 
 * The initialize method is called during the initialization of the
 * {@link FenixFramework}. This method is responsible for the initialization of
 * the {@link DomainMetaClass}es, and the {@link DomainConsistencyPredicates}.
 * It creates the persistent versions of new domain classes and predicates that
 * have been detected in the code, and deletes old ones that have been removed.
 * 
 * @author João Neves - JoaoRoxoNeves@ist.utl.pt
 **/
@NoDomainMetaObjects
public class DomainFenixFrameworkRoot extends DomainFenixFrameworkRoot_Base {

    public static final String ROOT_KEY = "pt.ist.fenixframework.pstm.DomainFenixFrameworkRoot";

    private static Map<Class<? extends AbstractDomainObject>, DomainClass> existingDMLDomainClasses;
    private static final Map<Class<? extends AbstractDomainObject>, DomainMetaClass> existingDomainMetaClasses = new HashMap<Class<? extends AbstractDomainObject>, DomainMetaClass>();

    public static DomainFenixFrameworkRoot getInstance() {
	return FenixFramework.getDomainFenixFrameworkRoot();
    }

    public DomainFenixFrameworkRoot() {
	super();
	checkIfIsSingleton();
    }

    /**
     * Verifies that there is no existing DomainFenixFrameworkRoot object.
     * Throws an {@link Error} otherwise. Used during the constructor of a new
     * <code>DomainFenixFrameworkRoot</code> object.
     * 
     * @throws Error
     *             if a <code>DomainFenixFrameworkRoot</code> object already
     *             exists.
     */
    private void checkIfIsSingleton() {
	if (FenixFramework.getDomainFenixFrameworkRoot() != null
		&& FenixFramework.getDomainFenixFrameworkRoot() != this) {
	    throw new Error("There can be only one instance of " + getClass().getSimpleName());
	}
    }

    public DomainMetaClass getDomainMetaClass(Class<? extends AbstractDomainObject> domainClass) {
	return existingDomainMetaClasses.get(domainClass);
    }

    /**
     * Removes a {@link DomainMetaClass} from the domain relation of existing
     * meta classes.
     */
    @Override
    public void removeDomainMetaClasses(DomainMetaClass metaClass) {
	checkFrameworkNotInitialized();
	Class<? extends AbstractDomainObject> domainClass = metaClass.getDomainClass();
	if (domainClass != null) {
	    existingDomainMetaClasses.remove(metaClass.getDomainClass());
	    existingDomainMetaClasses.remove(metaClass.getDomainClass().getSuperclass());
	}
	super.removeDomainMetaClasses(metaClass);
    }

    /**
     * Checks that the {@link FenixFramework} is not initialized, throws an
     * exception otherwise. Should be called before any changes are made to
     * {@link DomainMetaClass}es or to {@link DomainConsistencyPredicate}s.
     * 
     * @throws RuntimeException
     *             if the framework was already initialized
     */
    private void checkFrameworkNotInitialized() {
	if (FenixFramework.isInitialized()) {
	    throw new RuntimeException("Instances of " + getClass().getSimpleName()
		    + " cannot be edited after the FenixFramework has been initialized.");
	}
    }

    /**
     * Adds a {@link DomainMetaClass} to the domain relation of existing meta
     * classes.
     */
    @Override
    public void addDomainMetaClasses(DomainMetaClass metaClass) {
	checkFrameworkNotInitialized();
	existingDomainMetaClasses.put(metaClass.getDomainClass(), metaClass);
	// The metaClass for the base class is the same as the regular domain class
	existingDomainMetaClasses.put((Class<? extends AbstractDomainObject>) metaClass.getDomainClass().getSuperclass(),
		metaClass);
	super.addDomainMetaClasses(metaClass);
    }

    /**
     * Entry point of the initialization of the
     * <code>DomainFenixFrameworkRoot</code>, which initializes the
     * {@link DomainMetaClass}es and the {@link DomainConsistencyPredicate}s. If
     * the framework was configured not to create meta objects, it deletes all
     * {@link DomainMetaClass}es and {@link DomainConsistencyPredicate}s
     * instead.
     */
    public void initialize(DomainModel domainModel) {
	checkFrameworkNotInitialized();
	if (FenixFramework.canCreateDomainMetaObjects()) {
	    initializeDomainMetaClasses(domainModel);
	    initializeDomainConsistencyPredicates();
	    checkAllMethodsOverridingPredicates();
	} else {
	    deleteAllMetaObjectsAndClasses();
	}
    }

    /**
     * Initializes all the {@link DomainMetaClass}es by inspecting the domain
     * model.<br>
     * Identifies and processes:
     * <ul>
     * <li>old meta classes that no longer exist</li>
     * <li>existing meta classes that still exist</li>
     * <li>new meta classes that did not exist before</li>
     * </ul>
     */
    private void initializeDomainMetaClasses(DomainModel domainModel) {
	existingDMLDomainClasses = getExistingDomainClasses(domainModel);
	Set<DomainMetaClass> oldMetaClassesToRemove = new HashSet<DomainMetaClass>();

	for (DomainMetaClass metaClass : getDomainMetaClasses()) {
	    Class<? extends AbstractDomainObject> domainClass = metaClass.getDomainClass();
	    if ((domainClass == null) || (!existingDMLDomainClasses.keySet().contains(domainClass))) {
		oldMetaClassesToRemove.add(metaClass);
	    } else {
		existingDomainMetaClasses.put(domainClass, metaClass);
		//The base class has the same meta class as the regular domain class.
		existingDomainMetaClasses.put((Class<? extends AbstractDomainObject>) domainClass.getSuperclass(),
			metaClass);
	    }
	}

	if (!oldMetaClassesToRemove.isEmpty()) {
	    processOldClasses(oldMetaClassesToRemove);
	}

	if (!existingDomainMetaClasses.values().isEmpty()) {
	    processExistingMetaClasses(existingDomainMetaClasses.values());
	}

	Set<Class<? extends AbstractDomainObject>> newClassesToAddTopDown = new TreeSet<Class<? extends AbstractDomainObject>>(
		DomainMetaClass.COMPARATOR_BY_CLASS_HIERARCHY_TOP_DOWN);
	newClassesToAddTopDown.addAll(existingDMLDomainClasses.keySet());
	newClassesToAddTopDown.removeAll(existingDomainMetaClasses.keySet());
	if (!newClassesToAddTopDown.isEmpty()) {
	    processNewClasses(newClassesToAddTopDown);
	}
    }

    /**
     * @return a <code>Map</code> of <code>Classes</code> to {@link DomainClass}
     *         with all the existing classes of the domain model, except those
     *         annotated with {@link NoDomainMetaObjects}.
     */
    private Map<Class<? extends AbstractDomainObject>, DomainClass> getExistingDomainClasses(DomainModel domainModel) {
	Map<Class<? extends AbstractDomainObject>, DomainClass> existingDomainClasses = new HashMap<Class<? extends AbstractDomainObject>, DomainClass>();
	Iterator<DomainClass> domainClassesIterator = domainModel.getClasses();
	try {
	    while (domainClassesIterator.hasNext()) {
		DomainClass dmlDomainClass = domainClassesIterator.next();
		Class<? extends AbstractDomainObject> domainClass = (Class<? extends AbstractDomainObject>) Class
			.forName(dmlDomainClass.getFullName());

		if (!domainClass.getSuperclass().getName().equals(domainClass.getName() + "_Base")) {
		    throw new Error("The domain class: " + domainClass + " must extend its corresponding _Base class.");
		}
		if (domainClass.isAnnotationPresent(NoDomainMetaObjects.class)) {
		    continue;
		}

		existingDomainClasses.put(domainClass, dmlDomainClass);
	    }
	    return existingDomainClasses;
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	    throw new Error(e);
	}
    }

    /**
     * Processes the {@link DomainMetaClass}es that no longer exist in the
     * domain model, and invokes their deletion.
     * 
     * @param oldMetaClassesToProcess
     *            the <code>Collection</code> of {@link DomainMetaClass}es to be
     *            processed
     */
    private void processOldClasses(Collection<DomainMetaClass> oldMetaClassesToProcess) {
	deleteOldMetaClasses(oldMetaClassesToProcess);
    }

    /**
     * Deletes a <code>Collection</code> of {@link DomainMetaClass}es.
     * 
     * @param oldMetaClassesToRemove
     *            the <code>Collection</code> of {@link DomainMetaClass}es to be
     *            deleted
     */
    private void deleteOldMetaClasses(Collection<DomainMetaClass> oldMetaClassesToRemove) {
	for (DomainMetaClass metaClass : oldMetaClassesToRemove) {
	    metaClass.delete();
	}
    }

    /**
     * Processes the {@link DomainMetaClass}es that still exist in the domain
     * model, and invokes their update.
     * 
     * @param existingMetaClassesToProcess
     *            the <code>Collection</code> of {@link DomainMetaClass}es to be
     *            processed
     */
    private void processExistingMetaClasses(Collection<DomainMetaClass> existingMetaClassesToProcess) {
	finishInitializingMetaClasses(existingMetaClassesToProcess);
	updateExistingMetaClassHierarchy(existingMetaClassesToProcess);
    }

    /**
     * Finishes the initialization of any {@link DomainMetaClass}es that was not
     * yet finalized.<br>
     * 
     * @param existingMetaClassesToFinalize
     *            the <code>Collection</code> of {@link DomainMetaClass}es to be
     *            finalized
     */
    private void finishInitializingMetaClasses(Collection<DomainMetaClass> existingMetaClassesToFinalize) {
	for (DomainMetaClass metaClass : existingMetaClassesToFinalize) {
	    if (metaClass.isFinalized()) {
		continue;
	    }

	    metaClass.initExistingDomainObjects();

	    // Because the initExistingDomainObjects method is split among several transactions,
	    // the creation of the DomainMetaClass and its full initialization may not run atomically.
	    // In case of a system failure during the execution of the initExistingDomainObjects() method,
	    // the DomainMetaClass was already created, but not yet fully initialized.
	    // The init is only considered completed when finalized is set to true.
	    metaClass.setFinalized(true);
	}
    }

    /**
     * Updates a <code>Collection</code> of {@link DomainMetaClass}es.<br>
     * Each {@link DomainMetaClass} whose superclass changed will be deleted, so
     * that it can be reprocessed later as a new meta class.
     * 
     * @param existingMetaClassesToUpdate
     *            the <code>Collection</code> of {@link DomainMetaClass}es to be
     *            updated
     */
    private void updateExistingMetaClassHierarchy(Collection<DomainMetaClass> existingMetaClassesToUpdate) {
	for (DomainMetaClass metaClass : existingMetaClassesToUpdate) {
	    if (!metaClass.hasDomainMetaSuperclass()) {
		if (hasSuperclassInDML(metaClass)) {
		    System.out.println("[MetaClasses] MetaClass " + metaClass.getDomainClass().getSimpleName()
			    + " (and subclasses') hierarchy has changed...");
		    metaClass.delete();
		}
	    } else {
		DomainMetaClass currentMetaSuperclass = null;
		if (hasSuperclassInDML(metaClass)) {
		    currentMetaSuperclass = getDomainMetaSuperclassFromDML(metaClass);
		}
		if (currentMetaSuperclass != metaClass.getDomainMetaSuperclass()) {
		    System.out.println("[MetaClasses] MetaClass " + metaClass.getDomainClass().getSimpleName()
			    + " (and subclasses') hierarchy has changed...");
		    metaClass.delete();
		}
	    }
	}
    }

    /**
     * @return true if the {@link DomainMetaClass} has a superclass in the
     *         domain model
     */
    private boolean hasSuperclassInDML(DomainMetaClass metaClass) {
	DomainClass dmlDomainSuperclass = (DomainClass) existingDMLDomainClasses.get(metaClass.getDomainClass()).getSuperclass();
	return (dmlDomainSuperclass != null);
    }

    /**
     * @return the {@link DomainMetaClass} of the superclass of the metaClass
     *         passed as argument
     */
    private DomainMetaClass getDomainMetaSuperclassFromDML(DomainMetaClass metaClass) {
	try {
	    DomainClass dmlDomainSuperclass = (DomainClass) existingDMLDomainClasses.get(metaClass.getDomainClass())
		    .getSuperclass();
	    Class<? extends AbstractDomainObject> domainSuperclass = (Class<? extends AbstractDomainObject>) Class
		    .forName(dmlDomainSuperclass.getFullName());
	    return existingDomainMetaClasses.get(domainSuperclass);
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	    throw new Error(e);
	}
    }

    /**
     * Processes the Classes that did not exist before in the domain model, and
     * invokes the creation of new {@link DomainMetaClass}es and
     * {@link DomainMetaObject}s.<br>
     * The <code>Collection</code> of <code>Classes</code> passed as argument
     * MUST BE SORTED by the top-down order of their hierarchies. In this
     * <code>Collection</code> a superclass must always appear before all of its
     * subclasses.
     * 
     * @param newClassesToProcessTopDown
     *            the <code>Collection</code> of <code>Classes</code> to be
     *            processed, in top-down order of the class hiearchy
     */
    private void processNewClasses(Collection<Class<? extends AbstractDomainObject>> newClassesToProcessTopDown) {
	createNewMetaClasses(newClassesToProcessTopDown);
    }

    /**
     * Creates a {@link DomainMetaClass} for each new class. For each new
     * {@link DomainMetaClass}, initializes its meta-superclass and creates a
     * {@link DomainMetaObject} for each existing object of the new
     * {@link DomainMetaClass}.
     * 
     * @param newClassesToAddTopDown
     *            the <code>Collection</code> of Classes for which to create
     *            {@link DomainMetaClass}es, in top-down order
     */
    private void createNewMetaClasses(Collection<Class<? extends AbstractDomainObject>> newClassesToAddTopDown) {
	for (Class<? extends AbstractDomainObject> domainClass : newClassesToAddTopDown) {
	    // Commits the current, and starts a new write transaction.
	    // This is necessary to split the load of the mass creation of DomainMetaObjects among several transactions.
	    // Each transaction processes one DomainMetaClass.
	    Transaction.beginTransaction();
	    DomainMetaClass newDomainMetaClass = new DomainMetaClass(domainClass);
	    if (hasSuperclassInDML(newDomainMetaClass)) {
		newDomainMetaClass.initDomainMetaSuperclass(getDomainMetaSuperclassFromDML(newDomainMetaClass));
	    }
	    newDomainMetaClass.initExistingDomainObjects();

	    // Because the initExistingDomainObjects method is split among several transactions,
	    // the creation of the DomainMetaClass and its full initialization may not run atomically.
	    // In case of a system failure during the execution of the initExistingDomainObjects() method,
	    // the DomainMetaClass was already created, but not yet fully initialized.
	    // The init is only considered completed when finalized is set to true.
	    newDomainMetaClass.setFinalized(true);
	}
    }

    /**
     * Initializes all the {@link DomainConsistencyPredicate}s by inspecting the
     * domain Classes.<br>
     * Identifies and processes:
     * <ul>
     * <li>new predicates that did not exist before</li>
     * <li>old predicates that no longer exist</li>
     * <li>existing predicates that still exist</li>
     * </ul>
     */
    private void initializeDomainConsistencyPredicates() {
	Set<Method> newPredicatesToAdd = new HashSet<Method>();
	Set<DomainConsistencyPredicate> oldPredicatesToRemove = new HashSet<DomainConsistencyPredicate>();
	Map<Method, DomainConsistencyPredicate> existingDomainPredicates = new HashMap<Method, DomainConsistencyPredicate>();

	Set<DomainMetaClass> existingMetaClassesTopDown = new TreeSet<DomainMetaClass>(
		DomainMetaClass.COMPARATOR_BY_META_CLASS_HIERARCHY_TOP_DOWN);
	existingMetaClassesTopDown.addAll(existingDomainMetaClasses.values());
	for (DomainMetaClass metaClass : existingMetaClassesTopDown) {
	    Set<Method> existingPredicates = getDeclaredConsistencyPredicateMethods(metaClass);
	    for (DomainConsistencyPredicate declaredConsistencyPredicate : metaClass.getDeclaredConsistencyPredicates()) {
		Method predicateMethod = declaredConsistencyPredicate.getPredicate();
		if ((predicateMethod == null)
			|| (!predicateMethod.isAnnotationPresent(ConsistencyPredicate.class) && !predicateMethod
				.isAnnotationPresent(jvstm.cps.ConsistencyPredicate.class))) {
		    oldPredicatesToRemove.add(declaredConsistencyPredicate);
		} else {
		    existingDomainPredicates.put(declaredConsistencyPredicate.getPredicate(), declaredConsistencyPredicate);
		}
	    }

	    newPredicatesToAdd.addAll(existingPredicates);
	    newPredicatesToAdd.removeAll(existingDomainPredicates.keySet());

	    if (!newPredicatesToAdd.isEmpty()) {
		processNewPredicates(newPredicatesToAdd, metaClass);
		newPredicatesToAdd.clear();
	    }
	    if (!oldPredicatesToRemove.isEmpty()) {
		processOldPredicates(oldPredicatesToRemove, metaClass);
		oldPredicatesToRemove.clear();
	    }
	    if (!existingDomainPredicates.isEmpty()) {
		processExistingPredicates(existingDomainPredicates.values(), metaClass);
		existingDomainPredicates.clear();
	    }
	}
    }

    /**
     * @param metaClass
     *            the {@link DomainMetaClass} in which to look for consistency
     *            predicates
     * @return A <code>Set</code> of <code>Methods</code> annotated with
     *         {@link ConsistencyPredicate}, declared inside the domain class of
     *         the given metaClass. Excludes abstract <code>Methods</code>.
     * @throws Error
     *             if any predicate found has an invalid signature.
     */
    private Set<Method> getDeclaredConsistencyPredicateMethods(DomainMetaClass metaClass) {
	Class<? extends AbstractDomainObject> domainClass = metaClass.getDomainClass();
	Class<? extends AbstractDomainObject> baseClass = (Class<? extends AbstractDomainObject>) domainClass.getSuperclass();
	Set<Method> declaredMethods = getDeclaredConsistencyPredicateMethods(domainClass);
	declaredMethods.addAll(getDeclaredConsistencyPredicateMethods(baseClass));
	return declaredMethods;
    }

    /**
     * @param domainClass
     *            the class in which to look for consistency predicates
     * @return A <code>Set</code> of <code>Methods</code> annotated with
     *         {@link ConsistencyPredicate}, declared inside the given
     *         domainClass. Excludes abstract <code>Methods</code>.
     * @throws Error
     *             if any predicate found has an invalid signature.
     */
    private Set<Method> getDeclaredConsistencyPredicateMethods(Class<? extends AbstractDomainObject> domainClass) {
	Set<Method> declaredMethods = new HashSet<Method>();
	for (Method predicateMethod : domainClass.getDeclaredMethods()) {
	    if (!predicateMethod.isAnnotationPresent(ConsistencyPredicate.class)
		    && !predicateMethod.isAnnotationPresent(jvstm.cps.ConsistencyPredicate.class)) {
		continue;
	    }
	    if (predicateMethod.getParameterTypes().length != 0) {
		throw new Error("Consistency Predicates cannot have parameters - " + predicateMethod);
	    }
	    if (!predicateMethod.getReturnType().toString().equals("boolean")) {
		throw new Error("Consistency Predicates must return a primitive boolean value - " + predicateMethod);
	    }
	    if (!Modifier.isPrivate(predicateMethod.getModifiers()) && !Modifier.isProtected(predicateMethod.getModifiers())
		    && !Modifier.isPublic(predicateMethod.getModifiers())) {
		throw new Error("Consistency Predicates must be private, protected or public - " + predicateMethod);
	    }
	    if (Modifier.isAbstract(predicateMethod.getModifiers())) {
		continue;
	    }

	    declaredMethods.add(predicateMethod);
	}
	return declaredMethods;
    }

    /**
     * Processes the new consistency predicates that did not exist before in
     * this metaClass, and invokes the creation and execution of new
     * {@link DomainConsistencyPredicate}s.
     * 
     * @param metaClass
     *            the {@link DomainMetaClass} of the domain class that declares
     *            the predicates to process.
     * 
     * @param newPredicatesToProcess
     *            the <code>Set</code> of <code>Methods</code> annotated with
     *            {@link ConsistencyPredicate}, declared inside the domain class
     *            of the given metaClass.
     */
    private void processNewPredicates(Set<Method> newPredicatesToProcess, DomainMetaClass metaClass) {
	createAndExecuteNewPredicates(newPredicatesToProcess, metaClass);
    }

    /**
     * Creates a {@link DomainConsistencyPredicate} for each new predicate to
     * add. For each new {@link DomainConsistencyPredicate}, initializes any
     * possibly overridden predicate at a superclass, and executes the new
     * predicate for all affected objects.<br>
     * <strong>Assumes that the meta classes are being processed by the top-down
     * order of their hierarchy.</strong>
     * 
     * @param metaClass
     *            the {@link DomainMetaClass} of the domain class that declares
     *            the new predicates to add.
     * 
     * @param newPredicatesToAdd
     *            the <code>Set</code> of new <code>Methods</code> annotated
     *            with {@link ConsistencyPredicate}, declared inside the domain
     *            class of the given metaClass.
     */
    private void createAndExecuteNewPredicates(Set<Method> newPredicatesToAdd, DomainMetaClass metaClass) {
	for (Method predicateMethod : newPredicatesToAdd) {
	    // Commits the current, and starts a new write transaction.
	    // This is necessary to split the load of the mass creation of DomainDependenceRecords among several transactions.
	    // Each transaction fully processes one DomainConsistencyPredicate.
	    Transaction.beginTransaction();
	    DomainConsistencyPredicate newConsistencyPredicate = DomainConsistencyPredicate.createNewDomainConsistencyPredicate(
		    predicateMethod, metaClass);
	    newConsistencyPredicate.initConsistencyPredicateOverridden();
	    newConsistencyPredicate.executeConsistencyPredicateForMetaClassAndSubclasses(metaClass);
	}
    }

    /**
     * Processes the old {@link DomainConsistencyPredicate}s that no longer
     * exist in this metaClass, and invokes their deletion.
     * 
     * @param metaClass
     *            the {@link DomainMetaClass} of the domain class that used to
     *            declare the predicates to process.
     * 
     * @param oldPredicatesToProcess
     *            the <code>Set</code> of {@link DomainConsistencyPredicate}s,
     *            that are no longer being declared inside the domain class of
     *            the given metaClass.
     */
    private void processOldPredicates(Set<DomainConsistencyPredicate> oldPredicatesToProcess, DomainMetaClass metaClass) {
	deleteOldPredicates(oldPredicatesToProcess, metaClass);
    }

    /**
     * Deletes the old {@link DomainConsistencyPredicate}s that no longer exist
     * in this metaClass.
     * 
     * @param metaClass
     *            the {@link DomainMetaClass} of the domain class that used to
     *            declare the predicates to process.
     * 
     * @param oldPredicatesToProcess
     *            the <code>Set</code> of {@link DomainConsistencyPredicate}s,
     *            that are no longer being declared inside the domain class of
     *            the given metaClass.
     */
    private void deleteOldPredicates(Set<DomainConsistencyPredicate> oldPredicatesToRemove, DomainMetaClass metaClass) {
	for (DomainConsistencyPredicate knownConsistencyPredicate : oldPredicatesToRemove) {
	    // Commits the current, and starts a new write transaction.
	    // This is necessary to split the load of the mass deletion of DomainDependenceRecords among several transactions.
	    // Each transaction fully processes one DomainConsistencyPredicate.
	    Transaction.beginTransaction();
	    knownConsistencyPredicate.delete();
	}
    }

    /**
     * Processes the {@link DomainConsistencyPredicate}s that still exist in
     * this metaClass, and invokes their update.
     * 
     * @param metaClass
     *            the {@link DomainMetaClass} of the domain class that declares
     *            the predicates to process.
     * 
     * @param existingPredicatesToProcess
     *            the <code>Collection</code> of existing
     *            {@link DomainConsistencyPredicate}s, that are declared inside
     *            the domain class of the given metaClass.
     */
    private void processExistingPredicates(Collection<DomainConsistencyPredicate> existingPredicatesToProcess,
	    DomainMetaClass metaClass) {
	updateExistingPredicates(existingPredicatesToProcess, metaClass);
    }

    /**
     * Updates the overridden predicate of the
     * {@link DomainConsistencyPredicate}s that still exist in this metaClass.
     * This update is required because the predicates of a superclass might have
     * been changed, or even removed.
     * 
     * @param metaClass
     *            the {@link DomainMetaClass} of the domain class that declares
     *            the predicates to update.
     * 
     * @param existingPredicatesToUpdate
     *            the <code>Collection</code> of existing
     *            {@link DomainConsistencyPredicate}s, that are declared inside
     *            the domain class of the given metaClass.
     */
    private void updateExistingPredicates(Collection<DomainConsistencyPredicate> existingPredicatesToUpdate,
	    DomainMetaClass metaClass) {
	for (DomainConsistencyPredicate knownConsistencyPredicate : existingPredicatesToUpdate) {
	    knownConsistencyPredicate.updateConsistencyPredicateOverridden();
	}
    }

    /**
     * Checks that none of the current consistency predicates are being
     * overridden by regular methods. A method that overrides a consistency
     * predicate must also have the {@link ConsistencyPredicate} annotation.
     * 
     * @throws Error
     *             if any predicate is being overridden by a non-predicate
     *             method
     */
    private void checkAllMethodsOverridingPredicates() {
	for (DomainMetaClass metaClass : getDomainMetaClasses()) {
	    for (DomainConsistencyPredicate predicate : metaClass.getDeclaredConsistencyPredicates()) {
		predicate.checkOverridingMethods(metaClass);
	    }
	}
    }

    /**
     * Deletes all the {@link DomainMetaClass}es and {@link DomainMetaObject}s,
     * and any associated {@link DomainConsistencyPredicate}s and
     * {@link DomainDependenceRecord}s. This method should be invoked when the
     * {@link FenixFramework} is configured not to create meta objects.
     * 
     * @see Config#canCreateMetaObjects
     */
    private void deleteAllMetaObjectsAndClasses() {
	for (DomainMetaClass metaClass : getDomainMetaClasses()) {
	    // Commits the current, and starts a new write transaction.
	    // This is necessary to split the load of the mass deletion of objects among several transactions.
	    // Each transaction fully processes one DomainMetaClass.
	    Transaction.beginTransaction();
	    metaClass.massDelete();
	}
    }
}
