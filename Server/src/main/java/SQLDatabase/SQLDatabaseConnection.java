package SQLDatabase;

import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.CreateTableQuery;
import com.healthmarketscience.sqlbuilder.CustomCondition;
import com.healthmarketscience.sqlbuilder.DeleteQuery;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import com.healthmarketscience.sqlbuilder.InsertQuery;
import com.healthmarketscience.sqlbuilder.JdbcEscape;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.SqlObject;
import com.healthmarketscience.sqlbuilder.UpdateQuery;
import com.healthmarketscience.sqlbuilder.ValidationException;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;

import BasicCommonClasses.CatalogProduct;
import BasicCommonClasses.CustomerProfile;
import BasicCommonClasses.ForgetPassword;
import BasicCommonClasses.GroceryList;
import BasicCommonClasses.Ingredient;
import BasicCommonClasses.Location;
import BasicCommonClasses.Manufacturer;
import BasicCommonClasses.PlaceInMarket;
import BasicCommonClasses.ProductPackage;
import BasicCommonClasses.SmartCode;
import ClientServerApi.ClientServerDefs;
import CommonDefs.CLIENT_TYPE;
import SQLDatabase.SQLDatabaseEntities;
import SQLDatabase.SQLDatabaseEntities.CartsListTable;
import SQLDatabase.SQLDatabaseEntities.ClientsTable;
import SQLDatabase.SQLDatabaseEntities.CustomersIngredientsTable;
import SQLDatabase.SQLDatabaseEntities.CustomersTable;
import SQLDatabase.SQLDatabaseEntities.FreeIDsTable;
import SQLDatabase.SQLDatabaseEntities.GroceriesListsHistoryTable;
import SQLDatabase.SQLDatabaseEntities.GroceriesListsTable;
import SQLDatabase.SQLDatabaseEntities.IngredientsTable;
import SQLDatabase.SQLDatabaseEntities.LocationsTable;
import SQLDatabase.SQLDatabaseEntities.ManufacturerTable;
import SQLDatabase.SQLDatabaseEntities.ProductsCatalogIngredientsTable;
import SQLDatabase.SQLDatabaseEntities.ProductsCatalogLocationsTable;
import SQLDatabase.SQLDatabaseEntities.ProductsCatalogTable;
import SQLDatabase.SQLDatabaseEntities.ProductsPackagesTable;
import SQLDatabase.SQLDatabaseEntities.WorkersTable;
import SQLDatabase.SQLDatabaseException.*;
import SQLDatabase.SQLDatabaseStrings.LOCATIONS_TABLE;
import SQLDatabase.SQLDatabaseStrings.PRODUCTS_PACKAGES_TABLE;
import SQLDatabase.SQLDatabaseStrings.WORKERS_TABLE;
import UtilsImplementations.Serialization;

import static SQLDatabase.SQLQueryGenerator.generateSelectQuery1Table;
import static SQLDatabase.SQLQueryGenerator.generateSelectLeftJoinWithQuery2Tables;
import static SQLDatabase.SQLQueryGenerator.generateSelectInnerJoinWithQuery2Tables;
import static SQLDatabase.SQLQueryGenerator.generateUpdateQuery;
import static SQLDatabase.SQLQueryGenerator.generateDeleteQuery;

/**
 * SqlDBConnection - Handles the server request to the SQL database.
 * 
 * @author Noam Yefet
 * @since 2016-12-14
 * 
 */
public class SQLDatabaseConnection implements ISQLDatabaseConnection {

	static Logger log = Logger.getLogger(SQLDatabaseConnection.class.getName());

	private enum LOCATIONS_TYPES {
		WAREHOUSE, STORE, CART
	}

	/*
	 * Database parameters
	 */
	private static final String DATABASE_NAME = "SQLdatabase";
	private static final String DATABASE_PATH = "./src/main/resources/SQLDatabase/" + DATABASE_NAME;
	private static final String DATABASE_PARAMS = ";sql.syntax_mys=true;shutdown=true;hsqldb.write_delay=false";
	private static final String DATABASE_PATH_PARAMS = DATABASE_PATH + DATABASE_PARAMS;

	/*
	 * Queries parameters
	 */
	private static final String PARAM_MARK = "?";
	private static final String QUATED_PARAM_MARK = "'" + PARAM_MARK + "'";
	private static final String SQL_PARAM = "?";

	/**
	 * Define how many times to do random sessionID generation (and check if
	 * already such sessionID exist) before giving up
	 */
	private static final Integer TRYS_NUMBER = 1000;

	private Connection connection;
	private static boolean isEntitiesInitialized;

	public SQLDatabaseConnection() {

		// initialize entities object in first-run
		if (!isEntitiesInitialized) {
			SQLDatabaseEntities.initialize();
			isEntitiesInitialized = true;
		}

		// check if database exist on disk
		if (isDatabaseExists())
			// connect to database
			try {
				connection = DriverManager.getConnection("jdbc:hsqldb:file:" + DATABASE_PATH_PARAMS + ";ifexists=true",
						"SA", "");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		else {
			// connect and create database
			try {
				connection = DriverManager.getConnection("jdbc:hsqldb:file:" + DATABASE_PATH_PARAMS, "SA", "");
			} catch (SQLException e) {
				e.printStackTrace();
			}

			// creates tables
			createTables();
		}

	}

	/**
	 * Creates SQL tables
	 */
	private void createTables() {

		try {
			Statement statement = connection.createStatement();
			String createTableString = new CreateTableQuery(IngredientsTable.table, true).validate() + "";

			statement.executeUpdate(createTableString);
			createTableString = new CreateTableQuery(LocationsTable.table, true).validate() + "";
			statement.executeUpdate(createTableString);
			createTableString = new CreateTableQuery(ManufacturerTable.table, true).validate() + "";
			statement.executeUpdate(createTableString);
			createTableString = new CreateTableQuery(ProductsCatalogTable.table, true).validate() + "";
			statement.executeUpdate(createTableString);
			createTableString = new CreateTableQuery(ProductsCatalogIngredientsTable.table, true).validate() + "";
			statement.executeUpdate(createTableString);
			createTableString = new CreateTableQuery(ProductsCatalogLocationsTable.table, true).validate() + "";
			statement.executeUpdate(createTableString);
			createTableString = new CreateTableQuery(ProductsPackagesTable.table, true).validate() + "";
			statement.executeUpdate(createTableString);
			createTableString = new CreateTableQuery(GroceriesListsTable.table, true).validate() + "";
			statement.executeUpdate(createTableString);
			createTableString = new CreateTableQuery(GroceriesListsHistoryTable.table, true).validate() + "";
			statement.executeUpdate(createTableString);
			createTableString = new CreateTableQuery(CartsListTable.table, true).validate() + "";
			statement.executeUpdate(createTableString);
			createTableString = new CreateTableQuery(WorkersTable.workertable, true).validate() + "";
			statement.executeUpdate(createTableString);
			createTableString = new CreateTableQuery(FreeIDsTable.table, true).validate() + "";
			statement.executeUpdate(createTableString);
			createTableString = new CreateTableQuery(CustomersTable.customertable, true).validate() + "";
			statement.executeUpdate(createTableString);
			createTableString = new CreateTableQuery(CustomersIngredientsTable.table, true).validate() + "";
			statement.executeUpdate(createTableString);

		} catch (SQLException e) {

			e.printStackTrace();

		}

	}

	/**
	 * The method checks if SQL database is exist on local drive.
	 * 
	 * @return true if exist, false otherwise.
	 */
	private boolean isDatabaseExists() {

		// try to connect to database with "only-if-exist" parameter
		try {
			DriverManager.getConnection("jdbc:hsqldb:file:" + DATABASE_PATH_PARAMS + ";ifexists=true", "SA", "");
		} catch (SQLException e) {
			// if exception thrown - database not exists
			return false;
		}

		// else - database exists
		return true;
	}

	/**
	 * return the number of rows in specific ResultSet the cursor of the given
	 * reultset will point the beforeFirst row after that method
	 * 
	 * @param s
	 * @return the number of rows
	 */
	private static int getResultSetRowCount(ResultSet s) {
		if (s == null)
			return 0;
		try {
			s.last();
			return s.getRow();
		} catch (SQLException exp) {
			exp.printStackTrace();
		} finally {
			try {
				s.beforeFirst();
			} catch (SQLException exp) {
				exp.printStackTrace();
			}
		}
		return 0;
	}

	/**
	 * return if there any result in ResultsSet the cursor of the given reultset
	 * will point the beforeFirst row after that method
	 * 
	 * @param ¢
	 * @return false - if there are rows in the ResultsSet, true otherwise.
	 * @throws CriticalError
	 */
	private static boolean isResultSetEmpty(ResultSet ¢) throws CriticalError {
		boolean $;
		try {
			$ = ¢.first();
			¢.beforeFirst();

		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		}
		return !$;
	}

	/**
	 * Generate session id (for any client: worker, cart,..)
	 * 
	 * @return New session id (that not used before)
	 * @throws CriticalError
	 * @throws NumberOfConnectionsExceeded
	 */
	private int generateSessionID() throws CriticalError, NumberOfConnectionsExceeded {

		int minVal, maxVal;
		int $;

		minVal = 1;
		maxVal = Integer.MAX_VALUE;

		// trying to find available random session id.
		for (int ¢ = 0; ¢ < TRYS_NUMBER; ++¢) {
			// generate number between mivVal to maxVal (include)
			$ = new Random().nextInt(maxVal - minVal) + minVal;

			if (!isSessionExist($))
				return $;
		}

		throw new NumberOfConnectionsExceeded();

	}

	/**
	 * Allocate new ID for new row. (NOTE: used for manufaturerID, ingredientID
	 * and LocationID)
	 * 
	 * @param t
	 *            The table you want to insert new row in it
	 * @param c
	 *            The ID column of that table
	 * @return
	 * @throws CriticalError
	 */
	private int allocateIDToTable(DbTable t, DbColumn c) throws CriticalError {

		// search for free id
		String selectId = generateSelectQuery1Table(FreeIDsTable.table,
				BinaryCondition.equalTo(FreeIDsTable.fromTableNameCol, PARAM_MARK));

		PreparedStatement statement = null;
		ResultSet result = null;
		ResultSet maxIDResult = null;
		try {
			statement = getParameterizedReadQuery(selectId, t.getName());

			result = statement.executeQuery();
			int retID;

			if (!isResultSetEmpty(result)) {
				// return id from free_ids_table
				result.first();
				retID = result.getInt(FreeIDsTable.IDCol.getColumnNameSQL());

				// delete that id
				getParameterizedQuery(generateDeleteQuery(FreeIDsTable.table,
						BinaryCondition.equalTo(FreeIDsTable.fromTableNameCol, PARAM_MARK),
						BinaryCondition.equalTo(FreeIDsTable.IDCol, PARAM_MARK)), t.getName(), retID).executeUpdate();
			} else {
				// find max id and return the next number
				String maxIDQuery = new SelectQuery().addCustomColumns(FunctionCall.max().addColumnParams(c)).validate()
						+ "";

				maxIDResult = getParameterizedReadQuery(maxIDQuery).executeQuery();

				// if the table is empty - return 1
				if (isResultSetEmpty(result))
					retID = 1;
				else {
					maxIDResult.first();
					retID = maxIDResult.getInt(1) + 1;
				}

			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} finally {
			closeResources(statement, result, maxIDResult);
		}

		return 0;

	}

	/**
	 * Free ID when removing row. (NOTE: used for manufaturerID, ingredientID
	 * and LocationID)
	 * 
	 * @param t
	 *            The table you want to remove row from it.
	 * @param col
	 *            The ID column of that table
	 * @return
	 * @throws CriticalError
	 */
	private void freeIDOfTable(DbTable t, Integer idToFree) throws CriticalError {
		String insertQuery = new InsertQuery(FreeIDsTable.table).addColumn(FreeIDsTable.fromTableNameCol, PARAM_MARK)
				.addColumn(FreeIDsTable.IDCol, PARAM_MARK).validate() + "";

		PreparedStatement statement = getParameterizedQuery(insertQuery, t.getName(), idToFree);

		try {
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} finally {
			closeResources(statement);
		}

	}

	/**
	 * Validate if the client login the system
	 * 
	 * @param sessionID
	 *            - sessionID of the worker
	 * @throws ClientNotConnected
	 * @throws CriticalError
	 */
	private void validateSessionEstablished(Integer sessionID) throws ClientNotConnected, CriticalError {
		try {
			if (!isSessionExist(sessionID))
				throw new SQLDatabaseException.ClientNotConnected();
		} catch (ValidationException e) {
			e.printStackTrace();
			throw new CriticalError();
		}
	}

	/**
	 * Validate if the cart login the system
	 * 
	 * @param sessionID
	 *            - sessionID of the worker
	 * @throws ClientNotConnected
	 * @throws CriticalError
	 */
	private void validateCartSessionEstablished(Integer cartID) throws ClientNotConnected, CriticalError {
		try {
			if (!isCartSessionEstablished(cartID))
				throw new SQLDatabaseException.ClientNotConnected();
		} catch (ValidationException e) {
			e.printStackTrace();
			throw new CriticalError();
		}
	}

	/**
	 * Get the client type by session id. if no such session - return null
	 * IMPORTANT NOTE: this method returning the type ONLY IF the session is
	 * exist (and therefore connected) in the case that the client (like,
	 * worker) want to know its type - the method will return null even if the
	 * worker registered to the system. Still, he must be connected.
	 * 
	 * @param sessionID
	 * @return If this session is connected - returns client type. else -
	 *         returns null
	 * @throws CriticalError
	 */
	private CLIENT_TYPE getClientTypeBySessionID(Integer sessionID) throws CriticalError {

		if (sessionID == null)
			return null;

		String query = generateSelectQuery1Table(WorkersTable.workertable,
				BinaryCondition.equalTo(WorkersTable.workersessionIDCol, PARAM_MARK));

		PreparedStatement statement = getParameterizedReadQuery(query, sessionID);

		ResultSet result = null;
		try {
			log.debug("getClientTypeBySessionID: check if client is worker/manager \nby execute query: " + statement);
			result = statement.executeQuery();

			// CASE: worker/manager
			if (!isResultSetEmpty(result)) {

				result.first();

				int clientType = result.getInt(WorkersTable.workerPrivilegesCol.getColumnNameSQL());
				log.debug("getClientTypeBySessionID: worker found! worker type code from SQL: " + clientType);

				return clientType != WORKERS_TABLE.VALUE_PRIVILEGE_MANAGER ? CLIENT_TYPE.WORKER : CLIENT_TYPE.MANAGER;
			}
			
			// CASE: registered customer
			if (isSuchRowExist(CustomersTable.customertable, CustomersTable.customersessionIDCol, sessionID)) {
				log.debug("getClientTypeBySessionID: customer found!");
				return CLIENT_TYPE.CUSTOMER;
			}

			// CASE: anonymous customer
			if (isSuchRowExist(CartsListTable.table, CartsListTable.CartIDCol, sessionID)) {
				log.debug("getClientTypeBySessionID: anonymous customer found!");
				return CLIENT_TYPE.CART;
			}

			// CASE: none
			log.debug("getClientTypeBySessionID: no such id!");
			return null;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} finally {
			closeResources(statement, result);
		}

	}

	/**
	 * Check if the sessionID used in the system
	 * 
	 * @param sessionID
	 *            sessionID of the worker/custoemr
	 * @return true - if connected
	 * @throws CriticalError
	 */
	private boolean isSessionExist(Integer sessionID) throws CriticalError {
		return (sessionID == null) || (getClientTypeBySessionID(sessionID) != null);
	}

	/**
	 * Check if cart logged in the system
	 * 
	 * @param sessionID
	 *            sessionID of the worker/cart
	 * @return true - if connected
	 * @throws CriticalError
	 */
	private boolean isCartSessionEstablished(Integer cartID) throws CriticalError {
		return (cartID != null) || (getClientTypeBySessionID(cartID) == CLIENT_TYPE.CART);
	}

	/**
	 * Check if the worker logged in the system
	 * 
	 * @param username
	 *            username of the worker
	 * @return true - if connected
	 * @throws CriticalError
	 */
	private boolean isWorkerConnected(String username) throws CriticalError {

		return isClientConnected(new WorkersTable(), username);

	}
	
	/**
	 * 	
	 * General method to check if the customer or worker logged in the system
	 * 
	 * @param clientsTable - the relevant sql table to check in
	 * @param username username of the client
	 * @return true - if connected
	 * @throws CriticalError
	 */
	private boolean isClientConnected(ClientsTable t, String username) throws CriticalError {
		if (username == null)
			return true;

		ResultSet result = null;
		try {
			result = getParameterizedQuery(generateSelectQuery1Table(t.table,
					BinaryCondition.equalTo(t.usernameCol, PARAM_MARK)), username).executeQuery();

			if (isResultSetEmpty(result))
				return false;

			result.first();
			
			return result.getObject(t.sessionIDCol.getName()) != null;

		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} finally {
			closeResources(result);
		}
	}
	
	/**
	 * Check if the customer logged in the system
	 * 
	 * @param username
	 *            username of the customer
	 * @return true - if connected
	 * @throws CriticalError
	 */
	private boolean isCustomerConnected(String username) throws CriticalError {

		return isClientConnected(new CustomersTable(), username);

	}

	private String dateToString(LocalDate ¢) {
		return ¢.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
	}

	/**
	 * get parameterized query for execution.
	 * 
	 * @param query
	 *            - the query with parameter marks
	 * @param parameters
	 *            - the parameters to insert into the marks
	 * @return PreparedStatement of the parameterized query.
	 * @throws CriticalError
	 */
	private PreparedStatement getParameterizedQuery(String query, Object... parameters) throws CriticalError {

		query = query.replace(QUATED_PARAM_MARK, SQL_PARAM);

		PreparedStatement $;
		try {
			$ = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		}

		if (parameters != null)
			for (int ¢ = 0; ¢ < parameters.length; ++¢)
				try {
					$.setObject(¢ + 1, parameters[¢]);
				} catch (SQLException e) {
					e.printStackTrace();
					throw new CriticalError();
				}

		return $;

	}

	/**
	 * create parameterized read-only query for execution. usually used by
	 * SELECT queries.
	 * 
	 * @param query
	 *            - the query with parameter marks
	 * @param parameters
	 *            - the parameters to insert into the marks
	 * @return PreparedStatement of the parameterized query.
	 * @throws CriticalError
	 */
	private PreparedStatement getParameterizedReadQuery(String query, Object... parameters) throws CriticalError {

		query = query.replace(QUATED_PARAM_MARK, SQL_PARAM);

		PreparedStatement $;
		try {
			$ = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		}

		if (parameters != null)
			for (int ¢ = 0; ¢ < parameters.length; ++¢)
				try {
					$.setObject(¢ + 1, parameters[¢]);
				} catch (SQLException e) {
					e.printStackTrace();
					throw new CriticalError();
				}

		return $;

	}

	/**
	 * login method for worker/manager
	 * 
	 * @param username
	 * @param password
	 * @return new sessionID for connection
	 * @throws AuthenticationError
	 * @throws ClientAlreadyConnected
	 * @throws CriticalError
	 * @throws NumberOfConnectionsExceeded
	 */
	private int loginAsWorker(String username, String password)
			throws AuthenticationError, ClientAlreadyConnected, CriticalError, NumberOfConnectionsExceeded {
		String query = generateSelectQuery1Table(WorkersTable.workertable,
				BinaryCondition.equalTo(WorkersTable.workerusernameCol, PARAM_MARK),
				BinaryCondition.equalTo(WorkersTable.workerpasswordCol, PARAM_MARK));

		PreparedStatement statement = getParameterizedQuery(query, username, password);

		ResultSet result = null;
		try {
			result = statement.executeQuery();
			
			// check if no results or more than one - throw exception user not exist
			if (getResultSetRowCount(result) != 1)
				throw new SQLDatabaseException.AuthenticationError();

			// check if worker already connected
			if (isWorkerConnected(username))
				throw new SQLDatabaseException.ClientAlreadyConnected();

			/*
			 * EVERYTHING OK - initiate new session to worker
			 */
			int $ = generateSessionID();
			
			assignSessionIDToRegisteredClient(new WorkersTable(), username, $);

			return $;
			
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} finally {
			closeResources(statement, result);
		}


	}
	
	/**
	 * login method for customer
	 * 
	 * @param username
	 * @param password
	 * @return new sessionID for connection
	 * @throws AuthenticationError
	 * @throws ClientAlreadyConnected
	 * @throws CriticalError
	 * @throws NumberOfConnectionsExceeded
	 */
	private int loginAsCustomer(String username, String password)
			throws AuthenticationError, ClientAlreadyConnected, CriticalError, NumberOfConnectionsExceeded {
		String query = generateSelectQuery1Table(CustomersTable.customertable,
				BinaryCondition.equalTo(CustomersTable.customerusernameCol, PARAM_MARK),
				BinaryCondition.equalTo(CustomersTable.customerpasswordCol, PARAM_MARK));

		PreparedStatement statement = getParameterizedQuery(query, username, password);

		ResultSet result = null;
		try {
			result = statement.executeQuery();
			
			// check if no results or more than one - throw exception user not exist
			if (getResultSetRowCount(result) != 1)
				throw new SQLDatabaseException.AuthenticationError();

			// check if customer already connected
			if (isCustomerConnected(username))
				throw new SQLDatabaseException.ClientAlreadyConnected();

			/*
			 * EVERYTHING OK - initiate new session to customer and create new list for the customer
			 */
			int $ = generateSessionID();

			assignSessionIDToRegisteredClient(new CustomersTable(), username, $);
			initiateNewGroceryList($);
			
			return $;
		} catch (SQLException e) {
			e.printStackTrace();	
			throw new CriticalError();
		} finally {
			closeResources(statement, result);
		}

	}

	/**
	 * inserts sessionID to registered client (worker/customer)
	 * 
	 * @param t the table to assign the sessionID to
	 * @param username the username whose the sessionID is to be assigned
	 * @param sessionID the sessionID to assign
	 * @throws CriticalError
	 */
	private void assignSessionIDToRegisteredClient(ClientsTable t, String username, int sessionID) throws CriticalError {
		
		setValueToRegisteredClient(t, username, t.sessionIDCol, sessionID);
	}

	/**
	 * set one field to registered client (worker/customer)
	 * 
	 * 
	 * @param t the table to assign the new value to
	 * @param username the username whose the new value is to be assigned
	 * @param setColumn in which column assign the new value
	 * @param newValue the new value to assign
	 * @throws CriticalError
	 */
	private<T> void setValueToRegisteredClient(ClientsTable t,String username, DbColumn setColumn, T newValue) throws CriticalError {
		
		log.info("setValueoRegisteredClient: set: " + newValue + " to: " + setColumn.getColumnNameSQL() + " in table: " + t + " for user: " + username);
		PreparedStatement statement;
		UpdateQuery updateQuery = generateUpdateQuery(t.table,
				BinaryCondition.equalTo(t.usernameCol, PARAM_MARK));

		updateQuery.addSetClause(setColumn, PARAM_MARK).validate();
		
		//note: the username is last because in the query the order of paramerters is: set newValue and then Where username 
		statement = getParameterizedQuery(updateQuery + "", newValue, username);
		
		try {
			log.debug("setValueoRegisteredClient: run query: " + statement);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} finally {
			closeResources(statement);
		}
	}
	
	/**
	 *
	 * get one field of registered client (worker/customer)
	 * this method run select query to find relevant rows (where selectColumn == selectValue) and returns the value of getColumn
	 * 
	 * @param from the table to get the value from
	 * @param selectColumn the column to compare to selectValue 
	 * @param selectValue the value to compare to the selectColumn
	 * @param getColumn the column to fetch the desired value from
	 * 
	 * @return the desired value
	 * @throws CriticalError
	 */
	@SuppressWarnings("unchecked")
	private<E,T> T getValueForRegisteredClient(ClientsTable from, DbColumn selectColumn, E selectValue, DbColumn getColumn) throws CriticalError {
		
		//TODO: refactor all relevant methods to use this method

		log.info("getValueForRegisteredClient: trying to get value from tabel " + from);
		
		String query = generateSelectQuery1Table(from.table,
				BinaryCondition.equalTo(selectColumn, PARAM_MARK));

		PreparedStatement statement = getParameterizedReadQuery(query, selectValue);

		ResultSet result = null;
		try {
			log.debug("getValueForRegisteredClient: get desired value\nby execute query: " + statement);
			result = statement.executeQuery();

			if (isResultSetEmpty(result)){
				log.error("getValueForRegisteredClient: value not found");
				return null;
			}
			
			log.info("getValueForRegisteredClient: value found");
			
			//get the desired value
			result.first();
			return (T) result.getObject(getColumn.getColumnNameSQL());
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} finally {
			closeResources(statement, result);
		}
	}

	/**
	 * Create new user for cart and allocate new list for it
	 * 
	 * @return new sessionID for connection
	 * @throws CriticalError
	 * @throws NumberOfConnectionsExceeded
	 */
	private int loginAsCart() throws CriticalError, NumberOfConnectionsExceeded {

		/*
		 * initiate new session and new grocery list to cart
		 */
		int $ = generateSessionID();
		
		initiateNewGroceryList($);

		return $;
	}
	
	/**
	 * Creates new grocery list to the given sessionID and adds its to the database
	 * 
	 * @param sessionID the sessionID whose the new grocery list will be created to
	 * @throws CriticalError
	 */
	private void initiateNewGroceryList(int sessionID) throws CriticalError{
		
		// find max list id from grocery list table and history list table
		String maxListIDQuery = new SelectQuery()
				.addCustomColumns(FunctionCall.max().addColumnParams(CartsListTable.listIDCol)).validate() + "";
		String maxHistoryListIDQuery = new SelectQuery()
				.addCustomColumns(FunctionCall.max().addColumnParams(GroceriesListsHistoryTable.listIDCol)).validate()
				+ "";

		ResultSet maxListIDResult = null;
		ResultSet maxHistoryListIDResult = null;
		try {
			maxListIDResult = getParameterizedReadQuery(maxListIDQuery).executeQuery();
			maxHistoryListIDResult = getParameterizedReadQuery(maxHistoryListIDQuery).executeQuery();

			int maxListID = 0;
			int maxHistoryListID = 0;

			// get the max id from tables (if exist)
			if (!isResultSetEmpty(maxListIDResult)) {
				maxListIDResult.first();
				maxListID = maxListIDResult.getInt(1);
			}
			if (!isResultSetEmpty(maxHistoryListIDResult)) {
				maxHistoryListIDResult.first();
				maxHistoryListID = maxHistoryListIDResult.getInt(1);
			}

			maxListID = Math.max(maxListID, maxHistoryListID) + 1;

			// adding new cart connection to table
			String insertQuery = new InsertQuery(CartsListTable.table)
					.addColumn(CartsListTable.CartIDCol, PARAM_MARK)
					.addColumn(CartsListTable.listIDCol, PARAM_MARK).validate() + "";

			log.info("initiateNewGroceryList: run query: " + insertQuery);
			getParameterizedQuery(insertQuery, sessionID, maxListID).executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} finally {
			closeResources(maxHistoryListIDResult, maxListIDResult);
		}
			
	}

	/**
	 * set new password to registered client (worker/customer)
	 * 
	 * @param t the table to assign the sessionID to
	 * @param username the username whose the sessionID is to be assigned
	 * @param newPassword the new password to assign
	 * @throws CriticalError
	 */
	private void assignPasswordToRegisteredClient(ClientsTable t, String username, String newPassword) throws CriticalError {
		
		setValueToRegisteredClient(t, username, t.passwordCol, newPassword);
	}
	
	/**
	 * set new security question & answer to registered client (worker/customer)
	 * 
	 * @param t the table to assign the values to
	 * @param username the username whose the values is to be assigned
	 * @param forgetPassword object that contains the new values.
	 * @throws CriticalError
	 */
	private void assignSecurityQAToRegisteredClient(ClientsTable t, String username, ForgetPassword forgetPassword) throws CriticalError {
		setValueToRegisteredClient(t, username, t.securityQuestionCol, forgetPassword.getQuestion());
		setValueToRegisteredClient(t, username, t.securityAnswerCol, forgetPassword.getAnswer());
	}
	
	/**
	 * verify security answer of registered client (worker/customer).
	 * compare the real answer given by client with the real answer in the database
	 * 
	 * @param t the table to check the username in
	 * @param username the username whose his answer is to be checked
	 * @param givenAnswer the answer was given from the client
	 * 
	 * @return true - if the answers are same, false otherwise
	 * @throws CriticalError
	 */
	private boolean verifySecurityAnswerForRegisteredClient(ClientsTable t, String username, String givenAnswer) throws CriticalError {
		String realSecurityAnwser = getValueForRegisteredClient(t, t.usernameCol, username, t.securityAnswerCol);
		
		return realSecurityAnwser.equals(givenAnswer);
	}
	
	/**
	 * get security question of registered client (worker/customer)
	 * 
	 * @param t the table to get the username from
	 * @param username the username with the desired question
	 * 
	 * @return the security question of the given username
	 * @throws CriticalError
	 */
	private String getSecurityQuestionForRegisteredClient(ClientsTable t, String username) throws CriticalError {		
		return getValueForRegisteredClient(t, t.usernameCol, username, t.securityQuestionCol);
	}
	
	
	/**
	 * logout worker from system. the method disconnects worker with the given
	 * sessionID and username.
	 * 
	 * @param sessionID
	 * @param username
	 * @throws ClientNotConnected
	 * @throws CriticalError
	 */
	private void logoutAsWorker(Integer sessionID, String username) throws CriticalError {

		String query = generateSelectQuery1Table(WorkersTable.workertable,
				BinaryCondition.equalTo(WorkersTable.workerusernameCol, PARAM_MARK),
				BinaryCondition.equalTo(WorkersTable.workersessionIDCol, PARAM_MARK));

		PreparedStatement statement = getParameterizedQuery(query, username, sessionID);

		ResultSet result = null;
		try {

			result = statement.executeQuery();

			// check if no results or more than one - throw exception
			if (getResultSetRowCount(result) != 1)
				throw new SQLDatabaseException.CriticalError();

			/*
			 * EVERYTHING OK - disconnect worker
			 */
			UpdateQuery updateQuery = generateUpdateQuery(WorkersTable.workertable,
					BinaryCondition.equalTo(WorkersTable.workerusernameCol, PARAM_MARK));

			updateQuery.addSetClause(WorkersTable.workersessionIDCol, null).validate();

			statement = getParameterizedQuery(updateQuery + "", username);

			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} finally {
			closeResources(statement, result);
		}

	}

	/**
	 * logout cart from system. (remove cartID entry and it grocery list)
	 * 
	 * @param cartID
	 * @throws CriticalError
	 */
	private void logoutAsCart(int cartID) throws CriticalError {

		// READ part of transaction
		int listID = getCartListId(cartID);

		PreparedStatement deleteGroceryList = null;
		PreparedStatement deleteCart = null;
		try {
			// WRITE part of transaction

			// get grocery list and returns each product to shelf
			ResultSet groceryListResultSet = getGroceryListResultSetByCartID(cartID);
			groceryListResultSet.first();
			GroceryList groceryList = SQLJsonGenerator.resultSetToGroceryList(groceryListResultSet);
			if (groceryList.getList() != null)
				for (ProductPackage p : groceryList.getList().values())
					moveProductPackage(cartID, LOCATIONS_TYPES.CART, LOCATIONS_TYPES.STORE, p, p.getAmount());

			// delete grocery list and cart
			deleteGroceryList = getParameterizedQuery(generateDeleteQuery(GroceriesListsTable.table,
					BinaryCondition.equalTo(GroceriesListsTable.listIDCol, PARAM_MARK)), listID);
			deleteCart = getParameterizedQuery(generateDeleteQuery(CartsListTable.table,
					BinaryCondition.equalTo(CartsListTable.listIDCol, PARAM_MARK)), listID);

			log.debug("logoutAsCart: delete groceryList " + listID + ".\n by run query: " + deleteGroceryList);
			deleteGroceryList.executeUpdate();

			log.debug("logoutAsCart: disconnect cart " + cartID + ".\n by run query: " + deleteCart);
			deleteCart.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} catch (ProductPackageAmountNotMatch e) {
			log.error(
					"logoutAsCart: trying to return product to shelf but amount not matched to what in the grocery list");
			throw new CriticalError();
		} catch (ProductPackageNotExist e) {
			log.error("logoutAsCart: trying to return product to shelf but product package not exist in grocery list");
			throw new CriticalError();
		} finally {
			closeResources(deleteGroceryList, deleteCart);
		}
	}

	/**
	 * Change amount of product package. according to parameters, the method
	 * create/remove/update the relevant row
	 * 
	 * @param p
	 *            - product pacakge to update
	 * @param placeCol
	 *            - location's column name of the pacakage (can be
	 *            PRODUCTS_PACKAGES_TABLE.VALUE_PLACE_STORE or
	 *            PRODUCTS_PACKAGES_TABLE.VALUE_PLACE_WAREHOUSE)
	 * @param oldAmount
	 * @param newAmount
	 * @throws ProductPackageAmountNotMatch
	 * @throws CriticalError
	 */
	private void setNewAmountForStore(ProductPackage p, String placeCol, int oldAmount, int newAmount)
			throws ProductPackageAmountNotMatch, CriticalError {

		// case: not enough amount
		if (newAmount < 0)
			throw new ProductPackageAmountNotMatch();
		PreparedStatement statement = null;
		try {
			// case: add new row
			if (oldAmount == 0) {
				String insertQuery = new InsertQuery(ProductsPackagesTable.table)
						.addColumn(ProductsPackagesTable.barcodeCol, PARAM_MARK)
						.addColumn(ProductsPackagesTable.expirationDateCol,
								dateToString(p.getSmartCode().getExpirationDate()))
						.addColumn(ProductsPackagesTable.placeInStoreCol, PARAM_MARK)
						.addColumn(ProductsPackagesTable.amountCol, PARAM_MARK).validate() + "";

				insertQuery.hashCode();

				log.info("setNewAmountForStore: create new row amount to package: " + p + ", to place: " + placeCol);

				statement = getParameterizedQuery(insertQuery, p.getSmartCode().getBarcode(), placeCol, newAmount);

			} else if (newAmount == 0) { // case: remove row
				String deleteQuery = generateDeleteQuery(ProductsPackagesTable.table,
						BinaryCondition.equalTo(ProductsPackagesTable.barcodeCol, PARAM_MARK),
						BinaryCondition.equalTo(ProductsPackagesTable.placeInStoreCol, PARAM_MARK),
						BinaryCondition.equalTo(ProductsPackagesTable.expirationDateCol,
								JdbcEscape.date(Date.from(p.getSmartCode().getExpirationDate()
										.atStartOfDay(ZoneId.systemDefault()).toInstant()))));

				deleteQuery.hashCode();

				log.info("setNewAmountForStore: remove row to package: " + p + ", from place: " + placeCol);

				statement = getParameterizedQuery(deleteQuery, p.getSmartCode().getBarcode(), placeCol);

			} else { // case: update amount to new value
				UpdateQuery updateQuery = generateUpdateQuery(ProductsPackagesTable.table,
						BinaryCondition.equalTo(ProductsPackagesTable.barcodeCol, PARAM_MARK),
						BinaryCondition.equalTo(ProductsPackagesTable.placeInStoreCol, PARAM_MARK),
						BinaryCondition.equalTo(ProductsPackagesTable.expirationDateCol,
								JdbcEscape.date(Date.from(p.getSmartCode().getExpirationDate()
										.atStartOfDay(ZoneId.systemDefault()).toInstant()))));

				updateQuery.addSetClause(ProductsPackagesTable.amountCol, newAmount).validate();

				log.info("setNewAmountForStore: update row of package: " + p + ", of place: " + placeCol);

				statement = getParameterizedQuery(updateQuery + "", p.getSmartCode().getBarcode(), placeCol);
			}

			log.info("setNewAmountForStore : run query: " + statement);
			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} finally {
			closeResources(statement);
		}
	}

	/**
	 * Change amount of product package in cart. according to parameters, the
	 * method create/remove/update the relevant row
	 * 
	 * @param p
	 *            - product pacakge to update
	 * @param listID
	 *            - id of the grocery list to update
	 * @param oldAmount
	 * @param newAmount
	 * @throws ProductPackageAmountNotMatch
	 * @throws CriticalError
	 */
	private void setNewAmountForCart(ProductPackage p, Integer listID, int oldAmount, int newAmount)
			throws ProductPackageAmountNotMatch, CriticalError {

		// case: not enough amount
		if (newAmount < 0)
			throw new ProductPackageAmountNotMatch();

		PreparedStatement statement = null;
		try {
			// case: add new row
			if (oldAmount == 0) {
				String insertQuery = new InsertQuery(GroceriesListsTable.table)
						.addColumn(GroceriesListsTable.barcodeCol, PARAM_MARK)
						.addColumn(GroceriesListsTable.expirationDateCol,
								JdbcEscape.date(Date.from(p.getSmartCode().getExpirationDate()
										.atStartOfDay(ZoneId.systemDefault()).toInstant())))
						.addColumn(GroceriesListsTable.listIDCol, PARAM_MARK)
						.addColumn(GroceriesListsTable.amountCol, PARAM_MARK).validate() + "";

				insertQuery.hashCode();

				statement = getParameterizedQuery(insertQuery, p.getSmartCode().getBarcode(), listID, newAmount);

			} else if (newAmount == 0) { // case: remove row
				String deleteQuery = generateDeleteQuery(GroceriesListsTable.table,
						BinaryCondition.equalTo(GroceriesListsTable.barcodeCol, PARAM_MARK),
						BinaryCondition.equalTo(GroceriesListsTable.listIDCol, PARAM_MARK),
						BinaryCondition.equalTo(GroceriesListsTable.expirationDateCol,
								JdbcEscape.date(Date.from(p.getSmartCode().getExpirationDate()
										.atStartOfDay(ZoneId.systemDefault()).toInstant()))));

				deleteQuery.hashCode();

				statement = getParameterizedQuery(deleteQuery, p.getSmartCode().getBarcode(), listID);

			} else { // case: update amount to new value
				UpdateQuery updateQuery = generateUpdateQuery(GroceriesListsTable.table,
						BinaryCondition.equalTo(GroceriesListsTable.barcodeCol, PARAM_MARK),
						BinaryCondition.equalTo(GroceriesListsTable.listIDCol, PARAM_MARK),
						BinaryCondition.equalTo(GroceriesListsTable.expirationDateCol,
								JdbcEscape.date(Date.from(p.getSmartCode().getExpirationDate()
										.atStartOfDay(ZoneId.systemDefault()).toInstant()))));

				updateQuery.addSetClause(GroceriesListsTable.amountCol, newAmount).validate();

				statement = getParameterizedQuery(updateQuery + "", p.getSmartCode().getBarcode(), listID);
			}

			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} finally {
			closeResources(statement);
		}
	}

	/**
	 * Add product to the SQL database
	 * 
	 * @param p
	 *            New product to add
	 * @throws CriticalError
	 * @throws SQLException
	 */
	private void addCatalogProduct(CatalogProduct p) throws CriticalError, SQLException {

		// add all ingredients of product
		for (Ingredient ¢ : p.getIngredients()) {
			String insertToProductQuery = new InsertQuery(ProductsCatalogIngredientsTable.table)
					.addColumn(ProductsCatalogIngredientsTable.barcodeCol, PARAM_MARK)
					.addColumn(ProductsCatalogIngredientsTable.ingredientIDCol, PARAM_MARK).validate() + "";

			insertToProductQuery.hashCode();

			PreparedStatement statement = getParameterizedQuery(insertToProductQuery, p.getBarcode(), ¢.getId());

			statement.executeUpdate();

			closeResources(statement);

		}

		// add all locations of product
		for (Location ¢ : p.getLocations()) {
			int newID = allocateIDToTable(LocationsTable.table, LocationsTable.locationIDCol);
			String insertLocationQuery = new InsertQuery(LocationsTable.table)
					.addColumn(LocationsTable.locationIDCol, PARAM_MARK)
					.addColumn(LocationsTable.placeInStoreCol, PARAM_MARK)
					.addColumn(LocationsTable.pointXCol, PARAM_MARK).addColumn(LocationsTable.pointYCol, PARAM_MARK)
					.validate() + "";

			insertLocationQuery.hashCode();

			PreparedStatement insertLocationStatement = getParameterizedQuery(
					insertLocationQuery, newID, ¢.getPlaceInMarket().equals(PlaceInMarket.STORE)
							? LOCATIONS_TABLE.VALUE_PLACE_STORE : LOCATIONS_TABLE.VALUE_PLACE_WAREHOUSE,
					¢.getX(), ¢.getY());

			String insertToProductQuery = new InsertQuery(ProductsCatalogLocationsTable.table)
					.addColumn(ProductsCatalogLocationsTable.barcodeCol, PARAM_MARK)
					.addColumn(ProductsCatalogLocationsTable.locationIDCol, PARAM_MARK).validate() + "";

			PreparedStatement statement = getParameterizedQuery(insertToProductQuery, p.getBarcode(), newID);

			insertLocationStatement.executeUpdate();
			statement.executeUpdate();

			closeResources(insertLocationStatement);
			closeResources(statement);
		}

		// add the product itself
		String insertQuery = new InsertQuery(ProductsCatalogTable.table)
				.addColumn(ProductsCatalogTable.barcodeCol, PARAM_MARK)
				.addColumn(ProductsCatalogTable.manufacturerIDCol, PARAM_MARK)
				.addColumn(ProductsCatalogTable.productDescriptionCol, PARAM_MARK)
				.addColumn(ProductsCatalogTable.productNameCol, PARAM_MARK)
				.addColumn(ProductsCatalogTable.productPictureCol, PARAM_MARK)
				.addColumn(ProductsCatalogTable.productPriceCol, PARAM_MARK).validate() + "";

		PreparedStatement statement = getParameterizedQuery(insertQuery, p.getBarcode(), p.getManufacturer().getId(),
				p.getDescription(), p.getName(), p.getImageUrl(), p.getPrice());

		statement.executeUpdate();

		closeResources(statement);

	}

	/**
	 * Remove product from the SQL database (erase all associate entries in
	 * tables: Product catalog, Ingredients, Locations NOTE: other traces of the
	 * product will not be removed
	 * 
	 * @param p
	 *            - product to remove (only the barcode is used)
	 * @throws CriticalError
	 * @throws SQLException
	 */
	private void removeCatalogProduct(SmartCode p) throws CriticalError, SQLException {

		// remove all ingredients of product
		PreparedStatement statement = getParameterizedQuery(
				generateDeleteQuery(ProductsCatalogIngredientsTable.table,
						BinaryCondition.equalTo(ProductsCatalogIngredientsTable.barcodeCol, PARAM_MARK)),
				p.getBarcode());
		statement.executeUpdate();
		closeResources(statement);

		// remove all locations of product
		String selectAllLocationsQuery = new SelectQuery().addColumns(ProductsCatalogLocationsTable.locationIDCol)
				.addCondition(BinaryCondition.equalTo(ProductsCatalogLocationsTable.barcodeCol, PARAM_MARK)).validate()
				+ "";
		String deleteLocationsQuery = new DeleteQuery(LocationsTable.table)
				.addCondition(new CustomCondition(
						LocationsTable.locationIDCol.getColumnNameSQL() + " IN (" + selectAllLocationsQuery + " ) "))
				.validate() + "";

		PreparedStatement LocationsStatement = getParameterizedQuery(deleteLocationsQuery, p.getBarcode());
		LocationsStatement.executeUpdate();
		closeResources(LocationsStatement);

		// remove barcode form ProductsLocations Table
		PreparedStatement productLocationsStatement = getParameterizedQuery(
				generateDeleteQuery(ProductsCatalogLocationsTable.table,
						BinaryCondition.equalTo(ProductsCatalogLocationsTable.barcodeCol, PARAM_MARK)),
				p.getBarcode());
		productLocationsStatement.executeUpdate();
		closeResources(productLocationsStatement);

		// remove product itself
		PreparedStatement productStatement = getParameterizedQuery(generateDeleteQuery(ProductsCatalogTable.table,
				BinaryCondition.equalTo(ProductsCatalogTable.barcodeCol, PARAM_MARK)), p.getBarcode());
		productStatement.executeUpdate();
		closeResources(productStatement);

	}

	/**
	 * Get amount of relevant package in store
	 * 
	 * @param p
	 *            - product package
	 * @param placeCol
	 *            - location's column name of the pacakage (can be
	 *            PRODUCTS_PACKAGES_TABLE.VALUE_PLACE_STORE or
	 *            PRODUCTS_PACKAGES_TABLE.VALUE_PLACE_WAREHOUSE)
	 * @return
	 * @throws CriticalError
	 */
	private int getAmountForStore(ProductPackage p, String placeCol) throws CriticalError {
		String selectQuery = generateSelectQuery1Table(ProductsPackagesTable.table,
				BinaryCondition.equalTo(ProductsPackagesTable.barcodeCol, PARAM_MARK),
				BinaryCondition.equalTo(ProductsPackagesTable.placeInStoreCol, PARAM_MARK),
				BinaryCondition.equalTo(ProductsPackagesTable.expirationDateCol, JdbcEscape.date(Date
						.from(p.getSmartCode().getExpirationDate().atStartOfDay(ZoneId.systemDefault()).toInstant()))));

		PreparedStatement statement = getParameterizedReadQuery(selectQuery, p.getSmartCode().getBarcode(), placeCol);

		log.info("getAmountForStore: execute query: " + statement);

		ResultSet result = null;
		try {
			result = statement.executeQuery();

			if (isResultSetEmpty(result))
				return 0;

			result.first();

			return result.getInt(ProductsPackagesTable.amountCol.getColumnNameSQL());

		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} finally {
			closeResources(statement, result);
		}

	}

	/**
	 * Get amount of relevant package in cart
	 * 
	 * @param p
	 *            - product package
	 * @param listID
	 *            - id of the grocery list the package is in
	 * @return
	 * @throws CriticalError
	 * @throws ProductPackageNotExist
	 */
	private int getAmountForCart(ProductPackage p, Integer listID) throws CriticalError, ProductPackageNotExist {

		String selectQuery = generateSelectQuery1Table(GroceriesListsTable.table,
				BinaryCondition.equalTo(GroceriesListsTable.barcodeCol, PARAM_MARK),
				BinaryCondition.equalTo(GroceriesListsTable.listIDCol, PARAM_MARK),
				BinaryCondition.equalTo(GroceriesListsTable.expirationDateCol, JdbcEscape.date(Date
						.from(p.getSmartCode().getExpirationDate().atStartOfDay(ZoneId.systemDefault()).toInstant()))));

		log.info("execute query: " + selectQuery);

		PreparedStatement statement = getParameterizedReadQuery(selectQuery, p.getSmartCode().getBarcode(), listID);

		ResultSet result = null;
		try {
			result = statement.executeQuery();

			if (isResultSetEmpty(result))
				return 0;

			result.first();

			return result.getInt(GroceriesListsTable.amountCol.getColumnNameSQL());

		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} finally {
			closeResources(statement, result);
		}

	}

	/**
	 * Get grocery list is associate with cart NOTE: the method assuming cart
	 * already connected
	 * 
	 * @param cartId
	 * @return
	 * @throws CriticalError
	 */
	private int getCartListId(int cartId) throws CriticalError {

		String selectQuery = generateSelectQuery1Table(CartsListTable.table,
				BinaryCondition.equalTo(CartsListTable.CartIDCol, PARAM_MARK));

		PreparedStatement statement = getParameterizedReadQuery(selectQuery, cartId);

		ResultSet result = null;

		try {
			result = statement.executeQuery();

			result.first();
			return result.getInt(CartsListTable.listIDCol.getColumnNameSQL());

		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} finally {
			closeResources(statement, result);
		}

	}
	
	/**
	 * update the ingredients of the user to the ones in the hashset.
	 * 
	 * @param username The username of the customer to update.
	 * @param newSet The new set of ingredients to update to. cannot be null.
	 * @throws CriticalError
	 */
	private void updateIngredientsForCustomer(String username, HashSet<Ingredient> newSet) throws CriticalError {

		PreparedStatement statement = null;
		try {
			//remove the old list of ingredients
			String deleteIngredientsQuery = generateDeleteQuery(CustomersIngredientsTable.table,
					BinaryCondition.equalTo(CustomersIngredientsTable.customerUsernameCol, PARAM_MARK));
			
			statement = getParameterizedQuery(deleteIngredientsQuery, username);
	
			log.debug("updateIngredientsForCustomer: removing old ingredients of customer: " + username + "\nby running query: " + statement);
			statement.executeUpdate();
			closeResources(statement);
			
			
			for (Ingredient ingredient : newSet){
				//add new ingredients 
				String insertIngredientQuery = new InsertQuery(CustomersIngredientsTable.table)
						.addColumn(CustomersIngredientsTable.customerUsernameCol, PARAM_MARK)
						.addColumn(CustomersIngredientsTable.ingredientIDCol, PARAM_MARK).validate() + "";
				
				statement = getParameterizedQuery(insertIngredientQuery, username, ingredient.getId());
				
				log.debug("updateIngredientsForCustomer: add igredient: " + ingredient + " to customer: " + username + "\nby running query: statement");
				statement.executeUpdate();
				closeResources(statement);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} finally {
			closeResources(statement);
		}

	}
	

	/**
	 * Move product package form anywhere to anywhere
	 * 
	 * @param sessionId
	 * @param from
	 *            Take product package from LOCATIONS_TYPES
	 * @param to
	 *            And put it in LOCATIONS_TYPES
	 * @param packageToMove
	 *            Product Package to move (the method using only barcode and
	 *            exp. date)
	 * @param amount
	 *            Amount to transfer
	 * @throws CriticalError
	 * @throws ProductPackageAmountNotMatch
	 * @throws ProductPackageNotExist
	 */
	private void moveProductPackage(Integer sessionId, LOCATIONS_TYPES from, LOCATIONS_TYPES to,
			ProductPackage packageToMove, int amount)
			throws CriticalError, ProductPackageAmountNotMatch, ProductPackageNotExist {
		log.info("moveProductPackage: want to move Amount " + amount + " of Pacakge " + packageToMove + " From: " + from
				+ " To: " + to);

		if (from != null)
			switch (from) {
			case STORE: {
				int currentAmount = getAmountForStore(packageToMove, PRODUCTS_PACKAGES_TABLE.VALUE_PLACE_STORE);
				if (currentAmount == 0) {
					log.info("moveProductPackage: nothing to take from Store");
					throw new ProductPackageNotExist();
				}
				log.info("moveProductPackage: (from) Store have " + currentAmount + ", set to: "
						+ (currentAmount - amount));
				setNewAmountForStore(packageToMove, PRODUCTS_PACKAGES_TABLE.VALUE_PLACE_STORE, currentAmount,
						currentAmount - amount);
				break;
			}
			case WAREHOUSE: {
				int currentAmount = getAmountForStore(packageToMove, PRODUCTS_PACKAGES_TABLE.VALUE_PLACE_WAREHOUSE);
				if (currentAmount == 0) {
					log.info("moveProductPackage: nothing to take from Warehouse");
					throw new ProductPackageNotExist();
				}
				log.info("moveProductPackage: (from) Warehouse have " + currentAmount + ", set to: "
						+ (currentAmount - amount));
				setNewAmountForStore(packageToMove, PRODUCTS_PACKAGES_TABLE.VALUE_PLACE_WAREHOUSE, currentAmount,
						currentAmount - amount);
				break;
			}
			case CART: {
				if (sessionId == null) {
					log.fatal("moveProductPackage: you trying to move product from cart without sessionID. ABORT.");
					return;
				}
				int listID = getCartListId(sessionId);
				int currentAmount = getAmountForCart(packageToMove, listID);
				if (currentAmount == 0) {
					log.info("moveProductPackage: nothing to take from Cart");
					throw new ProductPackageNotExist();
				}
				log.info("moveProductPackage: (from) Cart have " + currentAmount + ", set to: "
						+ (currentAmount - amount));
				setNewAmountForCart(packageToMove, listID, currentAmount, currentAmount - amount);
				break;
			}
			}

		if (to != null)
			switch (to) {
			case STORE: {
				int currentAmount = getAmountForStore(packageToMove, PRODUCTS_PACKAGES_TABLE.VALUE_PLACE_STORE);
				log.info("moveProductPackage: (to) Store have " + currentAmount + ", set to: "
						+ (currentAmount + amount));
				setNewAmountForStore(packageToMove, PRODUCTS_PACKAGES_TABLE.VALUE_PLACE_STORE, currentAmount,
						currentAmount + amount);
				break;
			}
			case WAREHOUSE: {
				int currentAmount = getAmountForStore(packageToMove, PRODUCTS_PACKAGES_TABLE.VALUE_PLACE_WAREHOUSE);
				log.info("moveProductPackage: (to) Warehouse have " + currentAmount + ", set to: "
						+ (currentAmount + amount));
				setNewAmountForStore(packageToMove, PRODUCTS_PACKAGES_TABLE.VALUE_PLACE_WAREHOUSE, currentAmount,
						currentAmount + amount);
				break;
			}
			case CART: {
				if (sessionId == null) {
					log.fatal("moveProductPackage: you trying to move product to cart without sessionID. ABORT.");
					return;
				}
				int listID = getCartListId(sessionId);
				int currentAmount = getAmountForCart(packageToMove, listID);
				log.info("moveProductPackage: (to) Cart have " + currentAmount + ", set to: "
						+ (currentAmount + amount));
				setNewAmountForCart(packageToMove, listID, currentAmount, currentAmount + amount);
				break;
			}
			}
	}

	/**
	 * Determine if Object value found in table t at column c
	 * 
	 * @param t
	 * @param c
	 * @param value
	 * @return
	 * @throws CriticalError
	 */
	private boolean isSuchRowExist(DbTable t, DbColumn c, Object value) throws CriticalError {
		String prodctsTableQuery = generateSelectQuery1Table(t, BinaryCondition.equalTo(c, PARAM_MARK));

		PreparedStatement productStatement = getParameterizedReadQuery(prodctsTableQuery, value);

		ResultSet productResult = null;
		try {
			productResult = productStatement.executeQuery();
			return !isResultSetEmpty(productResult);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(productStatement, productResult);
		}

		// if somehow we got here - bad and throw exception
		throw new CriticalError();
	}

	private boolean isProductExistInCatalog(Long barcode) throws CriticalError {
		return isSuchRowExist(ProductsCatalogTable.table, ProductsCatalogTable.barcodeCol, barcode);
	}

	private boolean isManufacturerExist(Integer manufacturerID) throws CriticalError {
		return isSuchRowExist(ManufacturerTable.table, ManufacturerTable.manufacturerIDCol, manufacturerID);
	}

	private boolean isIngredientExist(long l) throws CriticalError {
		return isSuchRowExist(IngredientsTable.table, IngredientsTable.ingredientIDCol, l);
	}
	
	/**
	 * Check if all the ingredients in the set exist in the database
	 * 
	 * @param setToCheck set to check its ingredients
	 * @throws CriticalError
	 * @throws IngredientNotExist
	 */
	private void checkAllIngredientsExist(HashSet<Ingredient> setToCheck) throws CriticalError, IngredientNotExist {
		for (Ingredient ¢ : setToCheck)
			if (!isIngredientExist((int) ¢.getId()))
				throw new IngredientNotExist();
	}
	
	private boolean isCustomerExist(String username) throws CriticalError {
		return isSuchRowExist(CustomersTable.customertable, CustomersTable.customerusernameCol, username);
	}

	/**
	 * close opened resources.
	 * 
	 * @param resources
	 *            - list of resources to close.
	 */
	private void closeResources(AutoCloseable... resources) {
		for (AutoCloseable resource : resources)
			if (resource != null)
				try {
					resource.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
	}

	/**
	 * get a ResultSet object of a grocery list by cartID
	 * 
	 * @param cartId
	 *            - the cartID you need its grocery list
	 * @throws CriticalError
	 * @throws SQLException
	 */
	private ResultSet getGroceryListResultSetByCartID(int cartId) throws CriticalError, SQLException {

		String getGroceryListQuery = generateSelectInnerJoinWithQuery2Tables(CartsListTable.table,
				GroceriesListsTable.table, CartsListTable.listIDCol, CartsListTable.listIDCol,
				BinaryCondition.equalTo(CartsListTable.CartIDCol, PARAM_MARK));

		PreparedStatement statement = getParameterizedReadQuery(getGroceryListQuery, cartId);

		ResultSet result = statement.executeQuery();

		return result;

	}

	/*
	 * 
	 * Wrapping method for transaction operations. (I use it only to eliminate
	 * the ugly "try.. catch.." clause)
	 * 
	 */
	/**
	 * Start transaction
	 * 
	 * @param resources
	 *            - list of resources to close.
	 * @throws CriticalError
	 */
	private void connectionStartTransaction() throws CriticalError {
		try {
			connection.setAutoCommit(false);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		}
	}

	/**
	 * End transaction
	 * 
	 * @param resources
	 *            - list of resources to close.
	 * @throws CriticalError
	 */
	private void connectionEndTransaction() throws CriticalError {
		try {
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		}
	}

	/**
	 * Commit transaction
	 * 
	 * @param resources
	 *            - list of resources to close.
	 * @throws CriticalError
	 */
	private void connectionCommitTransaction() throws CriticalError {
		try {
			connection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		}
	}

	/**
	 * Rollback transaction
	 * 
	 * @param resources
	 *            - list of resources to close.
	 * @throws CriticalError
	 */
	private void connectionRollbackTransaction() throws CriticalError {
		try {
			connection.rollback();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		}
	}

	/*
	 * #####################################################################
	 * 
	 * 
	 * 
	 * 
	 * Public Methods
	 * 
	 * 
	 * 
	 * 
	 * #####################################################################
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see SQLDatabase.ISQLDatabaseConnection#WorkerLogin(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public int login(String username, String password)
			throws AuthenticationError, ClientAlreadyConnected, CriticalError, NumberOfConnectionsExceeded {

		log.info("SQL Public login: Worker trying to connect as: " + username);
		try {
			// START transaction
			connectionStartTransaction();

			int $ = ClientServerDefs.anonymousCustomerPassword.equals(password)
					& ClientServerDefs.anonymousCustomerUsername.equals(username)
					? loginAsCart() : loginAsWorker(username, password);

			// END transaction
			connectionCommitTransaction();

			return $;

		} catch (SQLDatabaseException e) {
			// NOTE: all exceptions flows here - for doing rollback
			e.printStackTrace();
			connectionRollbackTransaction();
			throw e;
		} finally {
			connectionEndTransaction();
		}
	}
	
	@Override
	public int loginCustomer(String username, String password)
			throws AuthenticationError, ClientAlreadyConnected, CriticalError, NumberOfConnectionsExceeded {
		log.info("SQL Public loginCustomer: Customer trying to connect as: " + username);
		try {
			// START transaction
			connectionStartTransaction();

			int $ = ClientServerDefs.anonymousCustomerPassword.equals(password)
					& ClientServerDefs.anonymousCustomerUsername.equals(username)
					? loginAsCart() : loginAsCustomer(username, password);

			// END transaction
			connectionCommitTransaction();

			return $;

		} catch (SQLDatabaseException e) {
			// NOTE: all exceptions flows here - for doing rollback
			e.printStackTrace();
			connectionRollbackTransaction();
			throw e;
		} finally {
			connectionEndTransaction();
		}
	}

	@Override
	public int loginWorker(String username, String password)
			throws AuthenticationError, ClientAlreadyConnected, CriticalError, NumberOfConnectionsExceeded {
		log.info("SQL Public loginWorker: Customer trying to connect as: " + username);
		try {
			// START transaction
			connectionStartTransaction();

			int $ = loginAsWorker(username, password);

			// END transaction
			connectionCommitTransaction();

			return $;

		} catch (SQLDatabaseException e) {
			// NOTE: all exceptions flows here - for doing rollback
			e.printStackTrace();
			connectionRollbackTransaction();
			throw e;
		} finally {
			connectionEndTransaction();
		}
	}

	@Override
	public String getClientType(Integer sessionID) throws ClientNotConnected, CriticalError {
		validateSessionEstablished(sessionID);

		log.info("SQL Public getClientType: Trying to get client type of: " + sessionID);

		return new Gson().toJson(getClientTypeBySessionID(sessionID));

	}
	
	@Override
	public void registerCustomer(String username, String password) throws CriticalError, ClientAlreadyExist{
		log.info("SQL Public registerCustomer: Customer trying to register with username: " + username);
		
		if (isCustomerExist(username)){
			log.info("SQL Public registerCustomer: already exist customer with username: " + username);
			throw new ClientAlreadyExist();
		}
		
		
		PreparedStatement statement = null;
		try {		
			// START transaction
			connectionStartTransaction();
			
			//Write part of transaction
			String insertCustomerQuery = new InsertQuery(CustomersTable.customertable)
					.addColumn(CustomersTable.customerusernameCol, PARAM_MARK)
					.addColumn(CustomersTable.customerpasswordCol, PARAM_MARK)
					.addColumn(CustomersTable.customerAddressCol, PARAM_MARK)
					.addColumn(CustomersTable.customerCityCol, PARAM_MARK)
					.addColumn(CustomersTable.customerEmailCol, PARAM_MARK)
					.addColumn(CustomersTable.customerFirstnameCol, PARAM_MARK)
					.addColumn(CustomersTable.customerLastnameCol, PARAM_MARK)
					.addColumn(CustomersTable.customerisLoggedInCol, PARAM_MARK)
					.addColumn(CustomersTable.customerPhonenumberCol, PARAM_MARK)
					.addColumn(CustomersTable.customersecurityAnswerCol, PARAM_MARK)
					.addColumn(CustomersTable.customersecurityQuestionCol, PARAM_MARK).validate() + "";

			statement = getParameterizedQuery(insertCustomerQuery, username, password,
					"", "", "", "", "", 0, "", "", "");

			statement.executeUpdate();

			// END transaction
			connectionCommitTransaction();

		} catch (SQLDatabaseException e) {
			// NOTE: all exceptions flows here - for doing rollback
			e.printStackTrace();
			connectionRollbackTransaction();
			throw e;
		} catch (SQLException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw new CriticalError();
		} finally {
			connectionEndTransaction();
			closeResources(statement);
		}
	}
	
	@Override
	public void setCustomerProfile(String username, CustomerProfile p) throws CriticalError, ClientNotExist, IngredientNotExist{
		log.info("SQL Public setCustomerProfile: Customer set profile: " + p + " to username: " + username);
		
		if (!isCustomerExist(username)){
			log.info("SQL Public setCustomerProfile: no such customer with username: " + username);
			throw new ClientNotExist();
		}
		
		checkAllIngredientsExist(p.getAllergens()); 
		
		PreparedStatement statement = null;
		try {		
			// START transaction
			connectionStartTransaction();
			
			//Write part of transaction
			//updating general info
			UpdateQuery updateQuery = generateUpdateQuery(CustomersTable.customertable,
					BinaryCondition.equalTo(CustomersTable.customerusernameCol, PARAM_MARK));
			updateQuery.addSetClause(CustomersTable.customerAddressCol, PARAM_MARK)
				.addSetClause(CustomersTable.customerCityCol, PARAM_MARK)
				.addSetClause(CustomersTable.customerEmailCol, PARAM_MARK)
				.addSetClause(CustomersTable.customerFirstnameCol, PARAM_MARK)
				.addSetClause(CustomersTable.customerLastnameCol, PARAM_MARK)
				.addSetClause(CustomersTable.customerPhonenumberCol, PARAM_MARK)
				.addSetClause(CustomersTable.customerBirthdateCol, 
						JdbcEscape.date(Date.from(p.getBirthdate()
						.atStartOfDay(ZoneId.systemDefault()).toInstant()))).validate();

			statement = getParameterizedQuery(updateQuery + "", username, 
					p.getStreet(), p.getCity(), p.getEmailAddress(), p.getFirstName(), p.getLastName(), p.getPhoneNumber());
 
			statement.executeUpdate();

			//updating ingredients of customer
			updateIngredientsForCustomer(username, p.getAllergens());
			
			log.info("SQL Public setCustomerProfile: Success setting profile for username: " + username);

			// END transaction
			connectionCommitTransaction();

		} catch (SQLDatabaseException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw e;
		} catch (SQLException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw new CriticalError();
		} finally {
			connectionEndTransaction();
			closeResources(statement);
		}
	}
	
	@Override
	public String getCustomerProfile(String username) throws CriticalError, ClientNotExist{
		log.info("SQL Public getCustomerProfile: Customer get profile: for username: " + username);
		
		if (!isCustomerExist(username)){
			log.info("SQL Public getCustomerProfile: no such customer with username: " + username);
			throw new ClientNotExist();
		}
		
		PreparedStatement selectCustomerStatement = null;
		PreparedStatement selectCustomerIngredientsStatement = null;
		ResultSet selectCustomerResult = null;
		ResultSet selectCustomerIngredientsResult = null;
		try {		
			
			//Read part of transaction
			String selectCustomerQuery = generateSelectQuery1Table(CustomersTable.customertable,
					BinaryCondition.equalTo(CustomersTable.customerusernameCol, PARAM_MARK));
			
			String selectCustomerIngredientsQuery = generateSelectInnerJoinWithQuery2Tables(
					CustomersIngredientsTable.table, IngredientsTable.table, CustomersIngredientsTable.ingredientIDCol,
					CustomersIngredientsTable.customerUsernameCol,
					BinaryCondition.equalTo(CustomersTable.customerusernameCol, PARAM_MARK));

			selectCustomerStatement = getParameterizedQuery(selectCustomerQuery + "", username);
			selectCustomerIngredientsStatement = getParameterizedQuery(selectCustomerIngredientsQuery + "", username);
 
			selectCustomerResult = selectCustomerStatement.executeQuery();
			selectCustomerResult.first();
			selectCustomerIngredientsResult = selectCustomerIngredientsStatement.executeQuery();

			String result = SQLJsonGenerator.CostumerProfileToJson(selectCustomerResult, selectCustomerIngredientsResult);
			log.info("SQL Public setCustomerProfile: Success getting profile for username: " + username);
			
			return result;

		} catch (SQLDatabaseException e) {
			throw e;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		} finally {
			closeResources(selectCustomerStatement, selectCustomerIngredientsStatement,
					selectCustomerResult, selectCustomerIngredientsResult);
		}
		
	}
	
	@Override
	public void setPasswordCustomer(String username, String newPassword) throws CriticalError, ClientNotExist{
		log.info("SQL Public setPasswordCustomer: Customer: " + username + " sets password.");
		
		if (!isCustomerExist(username)){
			log.info("SQL Public setPasswordCustomer: no such customer with username: " + username);
			throw new ClientNotExist();
		}
		
		try {		
			// START transaction
			connectionStartTransaction();
			
			//Write part of transaction
			//updating password
			assignPasswordToRegisteredClient(new CustomersTable(), username, newPassword);

			log.info("SQL Public setPasswordCustomer: Success setting password for username: " + username);

			// END transaction
			connectionCommitTransaction();
		} catch (SQLDatabaseException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw e;
		} finally {
			connectionEndTransaction();
		}
	}
	
	@Override
	public void setSecurityQACustomer(String username, ForgetPassword forgetPassword) throws CriticalError, ClientNotExist{
		log.info("SQL Public setSecurityQACustomer: Customer: " + username + " sets security Q&A.");
		
		if (!isCustomerExist(username)){
			log.info("SQL Public setSecurityQACustomer: no such customer with username: " + username);
			throw new ClientNotExist();
		}
		
		try {		
			// START transaction
			connectionStartTransaction();
			
			//Write part of transaction
			//updating security question and answer
			assignSecurityQAToRegisteredClient(new CustomersTable(), username, forgetPassword);

			log.info("SQL Public setSecurityQACustomer: Success setting ecurity Q&A for username: " + username);

			// END transaction
			connectionCommitTransaction();
		} catch (SQLDatabaseException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw e;
		} finally {
			connectionEndTransaction();
		}
	}
	
	@Override
	public String getSecurityQuestionCustomer(String username) throws CriticalError, ClientNotExist{
		log.info("SQL Public getSecurityQuestionCustomer: Customer: " + username + " get security question.");
		
		if (!isCustomerExist(username)){
			log.info("SQL Public getSecurityQuestionCustomer: no such customer with username: " + username);
			throw new ClientNotExist();
		}
		
		try {
			
			//Read part of transaction
			String result = getSecurityQuestionForRegisteredClient(new CustomersTable(), username);

			log.info("SQL Public getSecurityQuestionCustomer: the security question of user: " + username + " is: \n" + result);
			return result;

		} catch (SQLDatabaseException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	@Override
	public boolean verifySecurityAnswerCustomer(String username, String givenAnswer) throws CriticalError, ClientNotExist{
		log.info("SQL Public verifySecurityAnswerCustomer: Customer: " + username + " verify security answer.");
		
		if (!isCustomerExist(username)){
			log.info("SQL Public verifySecurityAnswerCustomer: no such customer with username: " + username);
			throw new ClientNotExist();
		}
		
		try {
			
			//Read part of transaction
			boolean result = verifySecurityAnswerForRegisteredClient(new CustomersTable(), username, givenAnswer);

			log.info("SQL Public verifySecurityAnswerCustomer: result of verification for user: " + username + " is: " + result);
			return result;

		} catch (SQLDatabaseException e) {
			e.printStackTrace();
			throw e;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see SQLDatabase.ISQLDatabaseConnection#WorkerLogout(java.lang.Integer,
	 * java.lang.String)
	 */
	@Override
	public void logout(Integer sessionID, String username) throws ClientNotConnected, CriticalError {

		log.info("SQL Public workerLogout: Client " + username + " trying to logout (SESSION: " + sessionID + " )");

		validateSessionEstablished(sessionID);

		// START transaction
		connectionStartTransaction();

		try {
			// WRITE part of transaction
			// determine the type of client
			if (getClientTypeBySessionID(sessionID) == CLIENT_TYPE.CART) {
				log.info("SQL Public workerLogout: logout as Cart");
				logoutAsCart(sessionID);
			} else {
				log.info("SQL Public workerLogout: logout as Worker/Manager");
				logoutAsWorker(sessionID, username);
			}

			// END transaction
			connectionCommitTransaction();
		} catch (CriticalError e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw e;
		} finally {
			connectionEndTransaction();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see SQLDatabase.ISQLDatabaseConnection#getProductFromCatalog(java.lang.
	 * Integer, long)
	 */
	@Override
	public String getProductFromCatalog(Integer sessionID, long barcode)
			throws ProductNotExistInCatalog, ClientNotConnected, CriticalError {

		log.info("SQL Public getProductFromCatalog: Trying to get product: " + barcode + " (SESSION: " + sessionID
				+ " )");

		validateSessionEstablished(sessionID);

		String prodctsIngredientsQuery = generateSelectLeftJoinWithQuery2Tables(ProductsCatalogIngredientsTable.table,
				IngredientsTable.table, IngredientsTable.ingredientIDCol, ProductsCatalogIngredientsTable.barcodeCol,
				BinaryCondition.equalTo(ProductsCatalogIngredientsTable.barcodeCol, PARAM_MARK));

		String prodctsLocationsQuery = generateSelectLeftJoinWithQuery2Tables(ProductsCatalogLocationsTable.table,
				LocationsTable.table, LocationsTable.locationIDCol, ProductsCatalogLocationsTable.barcodeCol,
				BinaryCondition.equalTo(ProductsCatalogLocationsTable.barcodeCol, PARAM_MARK));

		String prodctsTableQuery = generateSelectLeftJoinWithQuery2Tables(ProductsCatalogTable.table,
				ManufacturerTable.table, ManufacturerTable.manufacturerIDCol, ProductsCatalogTable.barcodeCol,
				BinaryCondition.equalTo(ProductsCatalogTable.barcodeCol, PARAM_MARK));

		PreparedStatement productStatement = getParameterizedReadQuery(prodctsTableQuery, Long.valueOf(barcode));
		PreparedStatement productIngredientsStatement = getParameterizedReadQuery(prodctsIngredientsQuery,
				Long.valueOf(barcode));
		PreparedStatement productLocationsStatement = getParameterizedReadQuery(prodctsLocationsQuery,
				Long.valueOf(barcode));

		ResultSet productResult = null;
		ResultSet ingredientResult = null;
		ResultSet locationsResult = null;

		try {
			// START transaction
			connectionStartTransaction();
			productResult = productStatement.executeQuery();
			ingredientResult = productIngredientsStatement.executeQuery();
			locationsResult = productLocationsStatement.executeQuery();

			// END transaction
			connectionCommitTransaction();

			// if no result - throw exception
			if (isResultSetEmpty(productResult))
				throw new SQLDatabaseException.ProductNotExistInCatalog();

			productResult.next();
			ingredientResult.next();
			locationsResult.next();
			return SQLJsonGenerator.ProductToJson(productResult, ingredientResult, locationsResult);

		} catch (SQLException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw new CriticalError();
		} finally {
			connectionEndTransaction();
			closeResources(productResult, ingredientResult, locationsResult);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * SQLDatabase.ISQLDatabaseConnection#AddProductPackageToWarehouse(java.lang
	 * .Integer, BasicCommonClasses.ProductPackage)
	 */
	@Override
	public void addProductPackageToWarehouse(Integer sessionID, ProductPackage p)
			throws CriticalError, ClientNotConnected, ProductNotExistInCatalog {

		log.info("SQL Public addProductPackageToWarehouse: with package " + p + " (SESSION: " + sessionID + " )");

		validateSessionEstablished(sessionID);

		try {
			// START transaction
			connectionStartTransaction();
			if (!isProductExistInCatalog(p.getSmartCode().getBarcode()))
				throw new ProductNotExistInCatalog();

			moveProductPackage(sessionID, null, LOCATIONS_TYPES.WAREHOUSE, p, p.getAmount());

			// END transaction
			connectionCommitTransaction();
		} catch (CriticalError | ProductPackageAmountNotMatch | ProductPackageNotExist e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			/*
			 * throw CriticalError instead of: ProductPackageAmountNotMatch |
			 * ProductPackageNotExist because its cant happen
			 */
			throw new CriticalError();
		} finally {
			connectionEndTransaction();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * SQLDatabase.ISQLDatabaseConnection#RemoveProductPackageToWarehouse(java.
	 * lang.Integer, BasicCommonClasses.ProductPackage)
	 */
	@Override
	public void removeProductPackageFromWarehouse(Integer sessionID, ProductPackage p) throws CriticalError,
			ClientNotConnected, ProductNotExistInCatalog, ProductPackageAmountNotMatch, ProductPackageNotExist {

		log.info("SQL Public removeProductPackageFromWarehouse: with package " + p + " (SESSION: " + sessionID + " )");
		validateSessionEstablished(sessionID);

		try {
			// START transaction
			connectionStartTransaction();
			if (!isProductExistInCatalog(p.getSmartCode().getBarcode()))
				throw new ProductNotExistInCatalog();

			moveProductPackage(sessionID, LOCATIONS_TYPES.WAREHOUSE, null, p, p.getAmount());

			// END transaction
			connectionCommitTransaction();
		} catch (CriticalError | ProductPackageAmountNotMatch | ProductPackageNotExist e) {
			// rollback transaction and throw the same error that occurred
			connectionRollbackTransaction();
			throw e;
		} finally {
			connectionEndTransaction();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * SQLDatabase.ISQLDatabaseConnection#AddProductToCatalog(java.lang.Integer,
	 * BasicCommonClasses.CatalogProduct)
	 */
	@Override
	public void addProductToCatalog(Integer sessionID, CatalogProduct productToAdd) throws CriticalError,
			ClientNotConnected, ProductAlreadyExistInCatalog, IngredientNotExist, ManufacturerNotExist {

		log.info("SQL Public addProductToCatalog: Trying to add: " + productToAdd + " (SESSION: " + sessionID + " )");
		validateSessionEstablished(sessionID);

		try {
			// START transaction
			connectionStartTransaction();

			// READ part of transaction
			if (isProductExistInCatalog(productToAdd.getBarcode()))
				throw new ProductAlreadyExistInCatalog();

			// check if manufacturer exist
			if (!isManufacturerExist((int) productToAdd.getManufacturer().getId()))
				throw new ManufacturerNotExist();

			// check if all ingredients exists
			checkAllIngredientsExist(productToAdd.getIngredients());

			// WRITE part of transaction
			addCatalogProduct(productToAdd);

			// END transaction
			connectionCommitTransaction();
		} catch (CriticalError | SQLException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw new CriticalError();
		} finally {
			connectionEndTransaction();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * SQLDatabase.ISQLDatabaseConnection#RemoveProductFromCatalog(java.lang.
	 * Integer, BasicCommonClasses.CatalogProduct)
	 */
	@Override
	public void removeProductFromCatalog(Integer sessionID, SmartCode productToRemove)
			throws CriticalError, ClientNotConnected, ProductNotExistInCatalog, ProductStillForSale {

		log.info("SQL Public removeProductFromCatalog: Trying to remove: " + productToRemove.getBarcode()
				+ " (SESSION: " + sessionID + " )");

		validateSessionEstablished(sessionID);

		try {
			// START transaction
			connectionStartTransaction();

			// READ part of transaction
			if (!isProductExistInCatalog(productToRemove.getBarcode()))
				throw new ProductNotExistInCatalog();

			// check if the product is in the system
			if (isSuchRowExist(ProductsPackagesTable.table, ProductsPackagesTable.barcodeCol,
					productToRemove.getBarcode())
					|| isSuchRowExist(GroceriesListsTable.table, GroceriesListsTable.barcodeCol,
							productToRemove.getBarcode())
					|| isSuchRowExist(GroceriesListsHistoryTable.table, GroceriesListsHistoryTable.barcodeCol,
							productToRemove.getBarcode()))
				throw new ProductStillForSale();

			// WRITE part of transaction
			removeCatalogProduct(productToRemove);

			// END transaction
			connectionCommitTransaction();
		} catch (CriticalError | SQLException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw new CriticalError();
		} finally {
			connectionEndTransaction();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * SQLDatabase.ISQLDatabaseConnection#HardRemoveProductFromCatalog(java.lang
	 * .Integer, BasicCommonClasses.CatalogProduct)
	 */
	@Override
	public void hardRemoveProductFromCatalog(Integer sessionID, CatalogProduct productToRemove)
			throws CriticalError, ClientNotConnected, ProductNotExistInCatalog {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see SQLDatabase.ISQLDatabaseConnection#UpdateProductInCatalog(java.lang.
	 * Integer, java.lang.Long, BasicCommonClasses.CatalogProduct)
	 */
	@Override
	public void editProductInCatalog(Integer sessionID, CatalogProduct productToUpdate) throws CriticalError,
			ClientNotConnected, ProductNotExistInCatalog, IngredientNotExist, ManufacturerNotExist {
		log.info("SQL Public editProductInCatalog: Trying to edit to: " + productToUpdate + " (SESSION: " + sessionID
				+ " )");

		validateSessionEstablished(sessionID);

		try {
			// START transaction
			connectionStartTransaction();

			// READ part of transaction
			if (!isProductExistInCatalog(productToUpdate.getBarcode()))
				throw new ProductNotExistInCatalog();

			// check if manufacturer exist
			if (isManufacturerExist((int) productToUpdate.getManufacturer().getId()))
				throw new ManufacturerNotExist();

			checkAllIngredientsExist(productToUpdate.getIngredients());

			// WRITE part of transaction
			// do update = remove product and adds it again
			removeCatalogProduct(new SmartCode(productToUpdate.getBarcode(), null));
			addCatalogProduct(productToUpdate);

			// END transaction
			connectionCommitTransaction();
		} catch (CriticalError | SQLException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw new CriticalError();
		} finally {
			connectionEndTransaction();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * SQLDatabase.ISQLDatabaseConnection#AddProductToGroceryList(java.lang.
	 * Integer, BasicCommonClasses.ProductPackage)
	 */
	@Override
	public void addProductToGroceryList(Integer cartID, ProductPackage productToBuy) throws CriticalError,
			ClientNotConnected, ProductNotExistInCatalog, ProductPackageAmountNotMatch, ProductPackageNotExist {
		log.info("SQL Public addProductToGroceryList: with parameter " + productToBuy + " (SESSION: " + cartID + " )");

		validateCartSessionEstablished(cartID);

		try {
			// START transaction
			connectionStartTransaction();

			// READ part of transaction
			if (!isProductExistInCatalog(productToBuy.getSmartCode().getBarcode()))
				throw new ProductNotExistInCatalog();

			// WRITE part of transaction
			moveProductPackage(cartID, LOCATIONS_TYPES.STORE, LOCATIONS_TYPES.CART, productToBuy,
					productToBuy.getAmount());

			// END transaction
			connectionCommitTransaction();
		} catch (CriticalError | ProductPackageAmountNotMatch | ProductPackageNotExist e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw e;
		} finally {
			connectionEndTransaction();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * SQLDatabase.ISQLDatabaseConnection#RemoveProductFromGroceryList(java.lang
	 * .Integer, BasicCommonClasses.ProductPackage)
	 */
	@Override
	public void removeProductFromGroceryList(Integer cartID, ProductPackage productToBuy) throws CriticalError,
			ClientNotConnected, ProductNotExistInCatalog, ProductPackageAmountNotMatch, ProductPackageNotExist {
		log.info("SQL Public removeProductFromGroceryList: with parameter " + productToBuy + " (SESSION: " + cartID
				+ " )");

		validateCartSessionEstablished(cartID);

		try {
			// START transaction
			connectionStartTransaction();

			// READ part of transaction
			if (!isProductExistInCatalog(productToBuy.getSmartCode().getBarcode()))
				throw new ProductNotExistInCatalog();

			// WRITE part of transaction
			moveProductPackage(cartID, LOCATIONS_TYPES.CART, LOCATIONS_TYPES.STORE, productToBuy,
					productToBuy.getAmount());

			// END transaction
			connectionCommitTransaction();
		} catch (CriticalError | ProductPackageAmountNotMatch | ProductPackageNotExist e) {
			connectionRollbackTransaction();
			throw e;
		} finally {
			connectionEndTransaction();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * SQLDatabase.ISQLDatabaseConnection#PlaceProductPackageOnShelves(java.lang
	 * .Integer, BasicCommonClasses.ProductPackage)
	 */
	@Override
	public void placeProductPackageOnShelves(Integer sessionID, ProductPackage p) throws CriticalError,
			ClientNotConnected, ProductNotExistInCatalog, ProductPackageAmountNotMatch, ProductPackageNotExist {

		log.info("SQL Public placeProductPackageOnShelves: with parameter " + p + " (SESSION: " + sessionID + " )");

		validateSessionEstablished(sessionID);

		try {
			// START transaction
			connectionStartTransaction();

			// READ part of transaction
			if (!isProductExistInCatalog(p.getSmartCode().getBarcode()))
				throw new ProductNotExistInCatalog();

			// WRITE part of transaction
			moveProductPackage(sessionID, LOCATIONS_TYPES.WAREHOUSE, LOCATIONS_TYPES.STORE, p, p.getAmount());

			// END transaction
			connectionCommitTransaction();
		} catch (CriticalError | ProductPackageAmountNotMatch | ProductPackageNotExist e) {
			connectionRollbackTransaction();
			throw e;
		} finally {
			connectionEndTransaction();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * SQLDatabase.ISQLDatabaseConnection#RemoveProductPackageFromShelves(java.
	 * lang.Integer, BasicCommonClasses.ProductPackage)
	 */
	@Override
	public void removeProductPackageFromShelves(Integer sessionID, ProductPackage p) throws CriticalError,
			ClientNotConnected, ProductNotExistInCatalog, ProductPackageAmountNotMatch, ProductPackageNotExist {
		log.info("SQL Public removeProductPackageFromShelves: with parameter " + p + " (SESSION: " + sessionID + " )");

		validateSessionEstablished(sessionID);

		try {
			// START transaction
			connectionStartTransaction();

			// READ part of transaction
			if (!isProductExistInCatalog(p.getSmartCode().getBarcode()))
				throw new ProductNotExistInCatalog();

			// WRITE part of transaction
			moveProductPackage(sessionID, LOCATIONS_TYPES.STORE, null, p, p.getAmount());

			// END transaction
			connectionCommitTransaction();
		} catch (CriticalError | ProductPackageAmountNotMatch | ProductPackageNotExist e) {
			connectionRollbackTransaction();
			throw e;
		} finally {
			connectionEndTransaction();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * SQLDatabase.ISQLDatabaseConnection#GetProductPackageAmonutOnShelves(java.
	 * lang.Integer, BasicCommonClasses.ProductPackage)
	 */
	@Override
	public String getProductPackageAmonutOnShelves(Integer sessionID, ProductPackage p)
			throws CriticalError, ClientNotConnected, ProductNotExistInCatalog {
		log.info("SQL Public getProductPackageAmonutOnShelves: with parameter " + p + " (SESSION: " + sessionID + " )");

		validateSessionEstablished(sessionID);

		return new Gson().toJson(getAmountForStore(p, PRODUCTS_PACKAGES_TABLE.VALUE_PLACE_STORE));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * SQLDatabase.ISQLDatabaseConnection#GetProductPackageAmonutInWarehouse(
	 * java.lang.Integer, BasicCommonClasses.ProductPackage)
	 */
	@Override
	public String getProductPackageAmonutInWarehouse(Integer sessionID, ProductPackage p)
			throws CriticalError, ClientNotConnected, ProductNotExistInCatalog {
		log.info("SQL Public getProductPackageAmonutInWarehouse: with parameter " + p + " (SESSION: " + sessionID
				+ " )");

		validateSessionEstablished(sessionID);

		return new Gson().toJson(getAmountForStore(p, PRODUCTS_PACKAGES_TABLE.VALUE_PLACE_WAREHOUSE));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see SQLDatabase.ISQLDatabaseConnection#cartCheckout(java.lang.Integer)
	 */
	@Override
	public void cartCheckout(Integer cartID) throws CriticalError, ClientNotConnected, GroceryListIsEmpty {
		log.info("SQL Public cartCheckout: of cart: " + cartID + " (SESSION: " + cartID + " )");

		validateCartSessionEstablished(cartID);

		// START transaction
		connectionStartTransaction();

		PreparedStatement copyStatement = null;
		PreparedStatement deleteGroceryList = null;
		ResultSet cartGroceryList = null;

		try {
			// READ part of transaction
			// check if grocery list of that cart is empty
			cartGroceryList = getGroceryListResultSetByCartID(cartID);

			if (isResultSetEmpty(cartGroceryList))
				throw new GroceryListIsEmpty();

			// everything ok - perform checkout
			int listID = getCartListId(cartID);

			// WRITE part of transaction
			// moving grocery list to history
			String copyQuery = "INSERT " + GroceriesListsHistoryTable.table.getTableNameSQL() + "( "
					+ GroceriesListsHistoryTable.listIDCol.getColumnNameSQL() + " , "
					+ GroceriesListsHistoryTable.barcodeCol.getColumnNameSQL() + " , "
					+ GroceriesListsHistoryTable.expirationDateCol.getColumnNameSQL() + " , "
					+ GroceriesListsHistoryTable.amountCol.getColumnNameSQL() + " ) "
					+ new SelectQuery()
							.addColumns(GroceriesListsTable.listIDCol, GroceriesListsTable.barcodeCol,
									GroceriesListsTable.expirationDateCol, GroceriesListsTable.amountCol)
							.addCondition(BinaryCondition.equalTo(GroceriesListsTable.listIDCol, PARAM_MARK))
							.validate();

			copyStatement = getParameterizedQuery(copyQuery, listID);
			deleteGroceryList = getParameterizedQuery(generateDeleteQuery(GroceriesListsTable.table,
					BinaryCondition.equalTo(GroceriesListsTable.listIDCol, PARAM_MARK)), listID);

			log.debug("cartCheckout: move groceryList " + listID + " to history.\n by run query: " + copyStatement
					+ "\n and: " + deleteGroceryList);
			copyStatement.executeUpdate();
			deleteGroceryList.executeUpdate();

			// logout cart
			logoutAsCart(cartID);

			// COMMIT transaction
			connectionCommitTransaction();
		} catch (SQLException | CriticalError e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw new CriticalError();
		} finally {
			connectionEndTransaction();
			closeResources(copyStatement, cartGroceryList, deleteGroceryList);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see SQLDatabase.ISQLDatabaseConnection#close()
	 */
	@Override
	public void close() throws CriticalError {

		log.info("SQL Public close.");
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLDatabaseException.CriticalError();
		}
	}

	@Override
	public String addManufacturer(Integer sessionID, String manufacturerName) throws CriticalError, ClientNotConnected {

		log.info("SQL Public addManufacturer: manufacturer name: " + manufacturerName + " (SESSION: " + sessionID
				+ " )");

		validateSessionEstablished(sessionID);

		int $;
		// START transaction
		connectionStartTransaction();
		try {
			// WRITE part of transaction
			// get "fresh" id for the new manufacturer
			$ = allocateIDToTable(ManufacturerTable.table, ManufacturerTable.manufacturerIDCol);

			String insertQuery = new InsertQuery(ManufacturerTable.table)
					.addColumn(ManufacturerTable.manufacturerIDCol, PARAM_MARK)
					.addColumn(ManufacturerTable.manufacturerNameCol, PARAM_MARK).validate() + "";

			insertQuery.hashCode();

			getParameterizedQuery(insertQuery, $, manufacturerName).executeUpdate();

			// END transaction
			connectionCommitTransaction();
		} catch (CriticalError | SQLException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw new CriticalError();
		} finally {
			connectionEndTransaction();
		}

		return new Gson().toJson(new Manufacturer($, manufacturerName));
	}

	@Override
	public void removeManufacturer(Integer sessionID, Manufacturer m)
			throws CriticalError, ClientNotConnected, ManufacturerNotExist, ManufacturerStillUsed {
		log.info("SQL Public removeManufacturer: manufacturer: " + m + " (SESSION: " + sessionID + " )");

		validateSessionEstablished(sessionID);

		// START transaction
		connectionStartTransaction();
		try {
			// READ part of transaction
			if (!isManufacturerExist((int) m.getId()))
				throw new ManufacturerNotExist();

			// if the manufacturer still used in catalog - throw exception
			if (isSuchRowExist(ProductsCatalogTable.table, ProductsCatalogTable.manufacturerIDCol, m.getId()))
				throw new ManufacturerStillUsed();

			// WRITE part of transaction
			// delete manufacturer
			getParameterizedQuery(generateDeleteQuery(ManufacturerTable.table,
					BinaryCondition.equalTo(ManufacturerTable.manufacturerIDCol, PARAM_MARK)), m.getId())
							.executeUpdate();

			// sign manufacturer's id as free
			freeIDOfTable(ManufacturerTable.table, (int) m.getId());

			// END transaction
			connectionCommitTransaction();
		} catch (CriticalError | SQLException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw new CriticalError();
		} finally {
			connectionEndTransaction();
		}

	}

	@Override
	public void editManufacturer(Integer sessionID, Manufacturer newManufacturer)
			throws CriticalError, ClientNotConnected, ManufacturerNotExist {
		log.info("SQL Public editManufacturer: edit to manufacturer: " + newManufacturer + " (SESSION: " + sessionID
				+ " )");

		validateSessionEstablished(sessionID);

		// START transaction
		connectionStartTransaction();
		try {
			// READ part of transaction
			if (!isManufacturerExist((int) newManufacturer.getId()))
				throw new ManufacturerNotExist();

			// WRITE part of transaction
			// update manufacturer
			UpdateQuery updateQuery = generateUpdateQuery(ManufacturerTable.table,
					BinaryCondition.equalTo(ManufacturerTable.manufacturerIDCol, PARAM_MARK));

			updateQuery.addSetClause(ManufacturerTable.manufacturerNameCol, PARAM_MARK).validate();

			getParameterizedQuery(updateQuery + "", newManufacturer.getId(), newManufacturer.getName()).executeUpdate();

			// END transaction
			connectionCommitTransaction();
		} catch (SQLException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw new CriticalError();
		} finally {
			connectionEndTransaction();
		}

	}
	
	@Override
	public String getManufacturersList(Integer sessionID) throws ClientNotConnected, CriticalError {
		validateSessionEstablished(sessionID);

		try {
			ResultSet manufacturerResultSet = getParameterizedReadQuery(
					new SelectQuery().addAllTableColumns(ManufacturerTable.table).validate() + "", (Object[]) null)
							.executeQuery();
			manufacturerResultSet.first();

			return SQLJsonGenerator.manufaturersListToJson(manufacturerResultSet);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		}
	}
	
	@Override
	public String addIngredient(Integer sessionID, String ingredientName) throws CriticalError, ClientNotConnected {

		log.info("SQL Public addIngredient: ingredient name: " + ingredientName + " (SESSION: " + sessionID
				+ " )");

		validateSessionEstablished(sessionID);

		int $;
		// START transaction
		connectionStartTransaction();
		try {
			// WRITE part of transaction
			// get "fresh" id for the new ingredient
			$ = allocateIDToTable(IngredientsTable.table, IngredientsTable.ingredientIDCol);

			String insertQuery = new InsertQuery(IngredientsTable.table)
					.addColumn(IngredientsTable.ingredientIDCol, PARAM_MARK)
					.addColumn(IngredientsTable.ingredientNameCol, PARAM_MARK).validate() + "";

			insertQuery.hashCode();

			getParameterizedQuery(insertQuery, $, ingredientName).executeUpdate();

			// END transaction
			connectionCommitTransaction();
		} catch (CriticalError | SQLException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw new CriticalError();
		} finally {
			connectionEndTransaction();
		}

		return Serialization.serialize(new Ingredient($, ingredientName));
	}

	@Override
	public void removeIngredient(Integer sessionID, Ingredient i)
			throws CriticalError, ClientNotConnected, IngredientNotExist, IngredientStillUsed {
		log.info("SQL Public removeIngredient: ingredient: " + i + " (SESSION: " + sessionID + " )");

		validateSessionEstablished(sessionID);

		// START transaction
		connectionStartTransaction();
		try {
			// READ part of transaction
			if (!isIngredientExist(i.getId()))
				throw new IngredientNotExist();

			// if the ingredient still used in catalog or customers - throw exception
			if (isSuchRowExist(ProductsCatalogIngredientsTable.table, ProductsCatalogIngredientsTable.ingredientIDCol, i.getId())
				|| isSuchRowExist(CustomersIngredientsTable.table, CustomersIngredientsTable.ingredientIDCol, i.getId()))
				throw new IngredientStillUsed();

			// WRITE part of transaction
			// delete manufacturer
			getParameterizedQuery(generateDeleteQuery(IngredientsTable.table,
					BinaryCondition.equalTo(IngredientsTable.ingredientIDCol, PARAM_MARK)), i.getId())
							.executeUpdate();

			// sign manufacturer's id as free
			freeIDOfTable(IngredientsTable.table, (int) i.getId());

			// END transaction
			connectionCommitTransaction();
		} catch (CriticalError | SQLException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw new CriticalError();
		} finally {
			connectionEndTransaction();
		}

	}

	@Override
	public void editIngredient(Integer sessionID, Ingredient newIngredient)
			throws CriticalError, ClientNotConnected, IngredientNotExist {
		log.info("SQL Public editIngredient: edit to ingredient: " + newIngredient + " (SESSION: " + sessionID
				+ " )");

		validateSessionEstablished(sessionID);

		// START transaction
		connectionStartTransaction();
		try {
			// READ part of transaction
			if (!isIngredientExist(newIngredient.getId()))
				throw new IngredientNotExist();

			// WRITE part of transaction
			// update manufacturer
			UpdateQuery updateQuery = generateUpdateQuery(IngredientsTable.table,
					BinaryCondition.equalTo(IngredientsTable.ingredientIDCol, PARAM_MARK));

			updateQuery.addSetClause(IngredientsTable.ingredientNameCol, PARAM_MARK).validate();

			getParameterizedQuery(updateQuery + "", newIngredient.getId(), newIngredient.getName()).executeUpdate();

			// END transaction
			connectionCommitTransaction();
		} catch (SQLException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw new CriticalError();
		} finally {
			connectionEndTransaction();
		}

	}
	
	@Override
	public String getIngredientsList() throws CriticalError {

		try {
			ResultSet ingredientsResultSet = getParameterizedReadQuery(
					new SelectQuery().addAllTableColumns(IngredientsTable.table).validate() + "", (Object[]) null)
							.executeQuery();
			
			ingredientsResultSet.first();

			return SQLJsonGenerator.allIngredientsListToJson(ingredientsResultSet);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CriticalError();
		}
	}

	@Override
	public void logoutAllUsers() throws CriticalError {
		log.info("SQL Public logoutAllUsers.");
		// START transaction
		connectionStartTransaction();

		PreparedStatement statement = null;
		try {
			// WRITE part of transaction
			// disconnect all carts
			statement = getParameterizedQuery(generateDeleteQuery(CartsListTable.table));
			log.debug("logoutAllUsers: clear carts.\n by using query: " + statement);
			statement.executeUpdate();
			closeResources(statement);

			// deletes all grocery lists
			//Todo - noam : when log out all users - the products not returns to shelf
			statement = getParameterizedQuery(generateDeleteQuery(GroceriesListsTable.table));
			log.debug("logoutAllUsers: delete grocery lists.\n by using query: " + statement);
			statement.executeUpdate();
			closeResources(statement);

			// disconnect all workers
			statement = getParameterizedQuery(new UpdateQuery(WorkersTable.workertable)
					.addSetClause(WorkersTable.workersessionIDCol, SqlObject.NULL_VALUE).validate() + "");
			log.debug("logoutAllUsers: logout workers.\n by using query: " + statement);
			statement.executeUpdate();
			closeResources(statement);

			// disconnect all customers
			statement = getParameterizedQuery(new UpdateQuery(CustomersTable.customertable)
					.addSetClause(CustomersTable.customersessionIDCol, SqlObject.NULL_VALUE).validate() + "");
			log.debug("logoutAllUsers: logout customers.\n by using query: " + statement);
			statement.executeUpdate();
			closeResources(statement);
			
			// END transaction
			connectionCommitTransaction();
		} catch (SQLException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw new CriticalError();
		} finally {
			connectionEndTransaction();
			closeResources(statement);
		}

	}

	@Override
	public boolean isClientLoggedIn(Integer sessionID) throws CriticalError {
		log.info("SQL Public isClientLoggedIn: sessionID: " + sessionID);
		return isSessionExist(sessionID);
	}

	@Override
	public boolean isWorkerLoggedIn(String username) throws CriticalError {
		log.info("SQL Public isWorkerLoggedIn: worker name: " + username);
		return isWorkerConnected(username);
	}

	@Override
	public String cartRestoreGroceryList(Integer cartID) throws CriticalError, NoGroceryListToRestore {

		log.info("SQL Public cartRestoreGroceryList: restore for cart: " + cartID + " (SESSION: " + cartID + " )");

		if (!isCartSessionEstablished(cartID))
			throw new NoGroceryListToRestore();

		// int listID = getCartListId(cartID);
		//
		// PreparedStatement statement =
		// getParameterizedReadQuery(generateSelectQuery1Table(GroceriesListsTable.table,
		// BinaryCondition.equalTo(GroceriesListsTable.listIDCol, PARAM_MARK)),
		// listID);

		log.debug("cartRestoreGroceryList: restoring grocery list.");

		ResultSet result = null;
		try {
			// result = statement.executeQuery();
			result = getGroceryListResultSetByCartID(cartID);
			result.first();
			return SQLJsonGenerator.GroceryListToJson(result);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(result);
		}

		return null;

	}

	@Override
	public void clearGroceryListsHistory() throws CriticalError {
		log.info("SQL Public clearGroceryListsHistory.");
		// START transaction
		connectionStartTransaction();

		PreparedStatement statement = null;
		try {
			// WRITE part of transaction

			// deletes all grocery lists in the history
			statement = getParameterizedQuery(generateDeleteQuery(GroceriesListsHistoryTable.table));
			log.debug("logoutAllUsers: delete grocery lists in history .\n by using query: " + statement);
			statement.executeUpdate();
			closeResources(statement);

			// END transaction
			connectionCommitTransaction();
		} catch (SQLException e) {
			e.printStackTrace();
			connectionRollbackTransaction();
			throw new CriticalError();
		} finally {
			connectionEndTransaction();
			closeResources(statement);
		}

	}

}
