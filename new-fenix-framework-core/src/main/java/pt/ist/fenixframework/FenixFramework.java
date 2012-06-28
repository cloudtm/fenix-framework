package pt.ist.fenixframework;

import java.io.Serializable;

// import jvstm.TransactionalCommand;
// import pt.ist.fenixframework.pstm.DataAccessPatterns;
// import pt.ist.fenixframework.pstm.MetadataManager;
// import pt.ist.fenixframework.core.PersistentRoot;
// import pt.ist.fenixframework.pstm.Transaction;
// import pt.ist.fenixframework.pstm.repository.RepositoryBootstrap;
// import pt.ist.fenixframework.core.Repository;
import pt.ist.fenixframework.dml.DomainModel;

/**
 * This class provides a method to initialize the entire Fenix Framework. To do
 * it, programmers should call the static <code>initialize(Config)</code> method
 * with a proper instance of the <code>Config</code> class.
 * 
 * After initialization, it is possible to get an instance of the
 * <code>DomainModel</code> class representing the structure of the
 * application's domain.
 * 
 * @see Config
 * @see dml.DomainModel
 */
public class FenixFramework {

    private static final Object INIT_LOCK = new Object();
    // private static boolean bootstrapped = false;
    private static boolean initialized = false;

    private static Config config;

    /**
     * @return whether the <code>FenixFramework.initialize</code> method has already been invoked
     */
    public static boolean isInitialized() {
	synchronized (INIT_LOCK) {
            return initialized;
        }
    }

    /** This method initializes the FenixFramework.  It must be the first method to be called, and
     * it should be invoked only once.  It needs to be called before starting to access any
     * Transactions/DomainObjects, etc.
     *
     * @param config The configuration that will be used by this instance of the framework.
     */
    public static void initialize(Config config) {
	synchronized (INIT_LOCK) {
	    if (initialized) {
		throw new Error("Fenix framework already initialized");
	    }

	    FenixFramework.config = ((config != null) ? config : new Config());
            config.initialize();

            // Because, the Config is an open extension point, we need to ensure the bare minimum,
            // e.g. a tx manager, an abstract domain object model, and a repository manager.
            // ensureConfigExtensionRequirements();

	    // MetadataManager.init(config);
	    // new RepositoryBootstrap(config).updateDataRepositoryStructureIfNeeded();
	    // DataAccessPatterns.init(config);
	    initialized = true;
	}

        // bootStrap(config);
        // initialize();
    }

    // // ensure that the minimum required components were setup
    // private static void ensureConfigExtensionRequirements() {
    //     Config.checkRequired(transactionManager, "transactionManager");
    //     Config.checkRequired(repository, "repository");
    // }

    // /** This method is public to allow other Fenix Framework extensions to invoke it, but it should
    //  * not be invoked by the programmer/user of this framework. */
    // public static void setTransactionManager(TransactionManager value) {
    //     // This method should only be invoked within FenixFramework.initialize(), but the
    //     // synchronized goes to ensure it.  Better safe than sorry.
    //     synchronized(INIT_LOCK) {
    //         if (transactionManager != null) {
    //             throw new Error("The 'transactionManager' is already set");
    //         }
    //         transactionManager = value;
    //     }
    // }
    
    // /** This method is public to allow other Fenix Framework extensions to invoke it, but it should
    //  * not be invoked by the programmer/user of this framework. */
    // public static void setRepository(Repository value) {
    //     // This method should only be invoked within FenixFramework.initialize(), but the
    //     // synchronized goes to ensure it.  Better safe than sorry.
    //     synchronized(INIT_LOCK) {
    //         if (repository != null) {
    //             throw new Error("The 'repository' is already set");
    //         }
    //         repository = value;
    //     }
    // }

    // /** This method is public to allow other Fenix Framework extensions to invoke it, but it should
    //  * not be invoked by the programmer/user of this framework. */
    // public static void setAbstractDomainObjectClass(Class<? implements DomainObject> value) {
    //     // This method should only be invoked within FenixFramework.initialize(), but the
    //     // synchronized goes to ensure it.  Better safe than sorry.
    //     synchronized(INIT_LOCK) {
    //         if (abstractDomainObjectClass != null) {
    //             throw new Error("The 'abstractDomainObjectClass' is already set");
    //         }
    //         abstractDomainObjectClass = value;
    //     }
    // }
    
    
    // private static void bootStrap(Config config) {
    //     synchronized (INIT_LOCK) {
    //         if (bootstrapped) {
    //     	throw new Error("Fenix framework already initialized");
    //         }

    //         FenixFramework.config = ((config != null) ? config : new Config());
    //         config.checkConfig();
    //         // MetadataManager.init(config);
    //         // new RepositoryBootstrap(config).updateDataRepositoryStructureIfNeeded();
    //         // DataAccessPatterns.init(config);
    //         bootstrapped = true;
    //     }
    // }

    // private static void initialize() {
    //     synchronized (INIT_LOCK) {
    //         if (initialized) {
    //     	throw new Error("Fenix framework already initialized");
    //         }
            

    //         // PersistentRoot.initRootIfNeeded(config);

    //         // FenixFrameworkPlugin[] plugins = config.getPlugins();
    //         // if (plugins != null) {
    //         //     for (final FenixFrameworkPlugin plugin : plugins) {
    //         //         Transaction.withTransaction(new TransactionalCommand() {
			
    //         //     	@Override
    //         //     	public void doIt() {
    //         //     	    plugin.initialize();
    //         //     	}

    //         //         });
    //         //     }
    //         // }
    //         initialized = true;
    //     }
    // }

    // private static ConfigurationExtension getConfigExtension() {
    //     return getConfig().getConfigExtension();
    // }

    public static Config getConfig() {
	return config;
    }

    public static TransactionManager getTransactionManager() {
        return getConfig().getConfigExtension().getTransactionManager();
    }

    // public static DomainModel getDomainModel() {
    //     return MetadataManager.getDomainModel();
    // }

    // public static <T extends DomainObject> T getRoot() {
    //     return (T) PersistentRoot.getRoot();
    // }


    public static <T extends DomainObject> T getDomainObject(Serializable externalId) {
        return getConfig().getConfigExtension().getDomainObject(externalId);
    }
}
