package com.gncompass.serverfront.db.model;

import com.gncompass.serverfront.api.model.UserEditable;
import com.gncompass.serverfront.api.model.UserViewable;
import com.gncompass.serverfront.db.InsertBuilder;
import com.gncompass.serverfront.db.SelectBuilder;
import com.gncompass.serverfront.db.UpdateBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class User extends AbstractObject {
  // Database name
  protected static final String TABLE_NAME = "Users";

  // Database column names
  private static final String ID = "id";
  private static final String TYPE = "type";
  private static final String PASSWORD = "password";
  private static final String PASSWORD_DATE = "password_date";
  private static final String NAME = "name";
  private static final String ENABLED = "enabled";
  private static final String FLAGS = "flags";
  private static final String ADDRESS1 = "address1";
  private static final String ADDRESS2 = "address2";
  private static final String ADDRESS3 = "address3";
  private static final String CITY = "city";
  private static final String PROVINCE = "province";
  private static final String POST_CODE = "post_code";
  private static final String COUNTRY = "country";
  private static final String CREATED = "created";

  // User types enumerator (all inherited children)
  public static enum UserType {
    BORROWER(1),
    INVESTOR(2);

    private final int value;

    private UserType(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  // Database parameters
  public long mId = 0;
  //public int mType = 0;
  public String mPassword = null;
  public Timestamp mPasswordDate = null;
  public String mName = null;
  public boolean mEnabled = false;
  public int mFlags = 0;
  public String mAddress1 = null;
  public String mAddress2 = null;
  public String mAddress3 = null;
  public String mCity = null;
  public String mProvince = null;
  public String mPostCode = null;
  public long mCountryId = 0;
  public Timestamp mCreated = null;

  // Internals
  private List<BankConnection> mBankConnections = null;

  protected User() {
  }

  protected User(String name, String passwordHash, Country country) {
    mName = name;
    mEnabled = true;
    mAddress1 = "";
    mCity = "";
    mCountryId = country.mId;

    mPassword = passwordHash;
  }

  /*=============================================================
   * PROTECTED FUNCTIONS
   *============================================================*/

  /**
   * Build the select SQL for all properties related to the borrower
   * @param childIdColumn the child ID column for the join
   * @param childTypeColumn the child type column for the join
   * @return the SelectBuilder reference object
   */
  protected SelectBuilder buildSelectParentSql(String childIdColumn, String childTypeColumn) {
    return buildSelectParentSql(childIdColumn, childTypeColumn, null);
  }

  /**
   * Build the select SQL for all properties related to the borrower
   * @param childIdColumn the child ID column for the join
   * @param childTypeColumn the child type column for the join
   * @param idColumn the parent ID match column for the join (optional)
   * @return the SelectBuilder reference object
   */
  protected SelectBuilder buildSelectParentSql(String childIdColumn, String childTypeColumn,
                                               String idColumn) {
    SelectBuilder selectBuilder = new SelectBuilder();
    selectBuilder.join(getTableParent(),
                       getColumnParent(ID) + "=" + childIdColumn + " AND " +
                       getColumnParent(TYPE) + "=" + childTypeColumn +
                       (idColumn != null ? " AND " + getColumnParent(ID) + "=" + idColumn : ""))
        .column(getColumnParent(ID))
        //.column(getColumnParent(TYPE))
        .column(getColumnParent(PASSWORD))
        .column(getColumnParent(PASSWORD_DATE))
        .column(getColumnParent(NAME))
        .column(getColumnParent(ENABLED))
        .column(getColumnParent(FLAGS))
        .column(getColumnParent(ADDRESS1))
        .column(getColumnParent(ADDRESS2))
        .column(getColumnParent(ADDRESS3))
        .column(getColumnParent(CITY))
        .column(getColumnParent(PROVINCE))
        .column(getColumnParent(POST_CODE))
        .column(getColumnParent(COUNTRY))
        .column(getColumnParent(CREATED));
    return selectBuilder;
  }

  /**
   * Returns the parent table name (this abstract class)
   * @return the object parent table name
   */
  @Override
  protected String getTableParent() {
    return TABLE_NAME;
  }

  /**
   * Updates this user information in the database. Requires that this user is valid with
   * a valid reference ID
   * @return TRUE if updated. FALSE otherwise
   * @throws SQLException on SQL failure
   */
  protected boolean updateDatabase(Connection conn) throws SQLException {
    if(mName != null && mAddress1 != null && mCity != null) {
      // Build the statement
      String updateSql = new UpdateBuilder(getTableParent())
          .set(getColumnParent(NAME) + "=?")
          .set(getColumnParent(ADDRESS1) + "=?")
          .set(getColumnParent(ADDRESS2) + (mAddress2 != null ? "=?" : "=NULL"))
          .set(getColumnParent(ADDRESS3) + (mAddress3 != null ? "=?" : "=NULL"))
          .set(getColumnParent(CITY) + "=?")
          .set(getColumnParent(PROVINCE) + (mProvince != null ? "=?" : "=NULL"))
          .set(getColumnParent(POST_CODE) + (mPostCode != null ? "=?" : "=NULL"))
          .where(getColumnParent(ID) + "=" + mId)
            .toString();

      // Borrower specific detail statement
      try (PreparedStatement statement = conn.prepareStatement(updateSql)) {
        int paramIndex = 1;
        statement.setString(paramIndex++, mName);
        statement.setString(paramIndex++, mAddress1);
        if (mAddress2 != null) {
          statement.setString(paramIndex++, mAddress2);
        }
        if (mAddress3 != null) {
          statement.setString(paramIndex++, mAddress3);
        }
        statement.setString(paramIndex++, mCity);
        if (mProvince != null) {
          statement.setString(paramIndex++, mProvince);
        }
        if (mPostCode != null) {
          statement.setString(paramIndex++, mPostCode);
        }

        // Update the user
        return (statement.executeUpdate() == 1);
      }
    }
    return false;
  }

  /**
   * Takes an editable user object received through the API and updates this class with the new
   * information
   * @param editable the editable data to match
   */
  protected void updateFromEditable(UserEditable editable) {
    mAddress1 = editable.mAddress1;
    mAddress2 = editable.mAddress2;
    mAddress3 = editable.mAddress3;
    mCity = editable.mCity;
    mName = editable.mName;
    mPostCode = editable.mPostCode;
    mProvince = editable.mProvince;
  }

  /**
   * Updates the viewable data set with the user data available in this parent
   * @param viewable the viewable data set
   * @param withConnectedInfo also include the connected info (banks, etc)
   */
  protected void updateViewable(UserViewable viewable, boolean withConnectedInfo) {
    // Fetch the country code
    Country country = new Country().getCountry(mCountryId);

    // Set the data
    viewable.setUserData(mName, mAddress1, mAddress2, mAddress3,
                            mCity, mProvince, mPostCode, country != null ? country.mCode : null);
    if (withConnectedInfo) {
      viewable.setBankData(mBankConnections);
    }
  }

  /*=============================================================
   * PACKAGE-PRIVATE FUNCTIONS
   *============================================================*/

  /**
   * Updates the user info from the result set provided. This assumes it was fetched appropriately
   * by one of the child connected tables
   * @param resultSet the result set to pull the data from. This will not call .next()
   * @throws SQLException if the data is unexpected in the result set
   */
  @Override
  void updateFromFetch(ResultSet resultSet) throws SQLException {
    mId = resultSet.getLong(getColumnParent(ID));
    //mType = resultSet.getInt(getColumnParent(TYPE));
    mPassword = resultSet.getString(getColumnParent(PASSWORD));
    mPasswordDate = resultSet.getTimestamp(getColumnParent(PASSWORD_DATE));
    mName = resultSet.getString(getColumnParent(NAME));
    mEnabled = resultSet.getBoolean(getColumnParent(ENABLED));
    mFlags = resultSet.getInt(getColumnParent(FLAGS));
    mAddress1 = resultSet.getString(getColumnParent(ADDRESS1));
    mAddress2 = resultSet.getString(getColumnParent(ADDRESS2));
    mAddress3 = resultSet.getString(getColumnParent(ADDRESS3));
    mCity = resultSet.getString(getColumnParent(CITY));
    mProvince = resultSet.getString(getColumnParent(PROVINCE));
    mPostCode = resultSet.getString(getColumnParent(POST_CODE));
    mCountryId = resultSet.getLong(getColumnParent(COUNTRY));
    mCreated = resultSet.getTimestamp(getColumnParent(CREATED));
  }

  /*=============================================================
   * PUBLIC FUNCTIONS
   *============================================================*/

  /**
   * Adds the user to the database
   * @param conn the SQL connection
   * @return TRUE if successfully added. FALSE otherwise
   * @throws SQLException exception on insert
   */
  public boolean addToDatabase(Connection conn) throws SQLException {
    if (mPassword != null && mName != null && mAddress1 != null && mCity != null &&
        mCountryId > 0) {
      // Create the user insert statement
      String insertSql = new InsertBuilder(getTableParent())
          .set(TYPE, Integer.toString(getUserType().getValue()))
          .setString(PASSWORD, mPassword)
          .set(NAME, "?")
          .setString(ADDRESS1, mAddress1)
          .setString(CITY, mCity)
          .setString(COUNTRY, Long.toString(mCountryId))
          .toString();

      // Execute the insert
      try (PreparedStatement statement = conn.prepareStatement(insertSql)) {
        statement.setString(1, mName);
        if (statement.executeUpdate() == 1) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Fetches a subset of connected information, such as bank connections that will be stored
   * within this object
   */
  public void fetchConnectedInfo() {
    mBankConnections = BankConnection.getAllForUser(this);
  }

  /**
   * Returns the user table ID
   * @return the user child table ID
   */
  public long getUserId() {
    return mId;
  }

  /**
   * Returns the user reference (abstract)
   * @return the user reference UUID
   */
  public abstract UUID getUserReference();

  /**
   * Returns the user type (abstract)
   * @return the user type enum
   */
  public abstract UserType getUserType();

  /**
   * Returns the viewable API JSON container (abstract)
   * @param withConnectedInfo also include the connected info (assessments, banks, etc)
   * @return the user viewable API object
   */
  public abstract UserViewable getViewable(boolean withConnectedInfo);

  /**
   * Returns if the user reference matches the string UUID provided (abstract)
   * @param userReference the user UUID reference
   * @return TRUE if matches. FALSE if doesn't
   */
  public abstract boolean matches(String userReference);

  /**
   * Identifies if the type and reference provided matches the user
   * @param type the user type
   * @param userReference the user UUID reference
   * @return TRUE if matches. FALSE if doesn't
   */
  public boolean matches(UserType type, String userReference) {
    return (getUserType() == type && matches(userReference));
  }
}
