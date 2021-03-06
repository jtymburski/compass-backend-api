package com.gncompass.serverfront.db.model;

import com.gncompass.serverfront.db.SelectBuilder;
import com.gncompass.serverfront.db.SQLManager;
import com.gncompass.serverfront.util.PaymentHelper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LoanAmortization extends AbstractObject {
  // Database name
  private static final String TABLE_NAME = "LoanAmortizations";

  // Database column names
  private static final String ID = "id";
  private static final String NAME = "name";
  private static final String MONTHS = "months";

  // Database parameters
  public long mId = 0;
  public String mName = null;
  public int mMonths = 0;

  public LoanAmortization() {
  }

  public LoanAmortization(ResultSet rs) throws SQLException {
    updateFromFetch(rs);
  }

  /*=============================================================
   * PRIVATE FUNCTIONS
   *============================================================*/

  /**
   * Adds the columns to the select builder provided
   * @param selectBuilder the select builder to add to
   * @return the modified select builder
   */
  private SelectBuilder addColumns(SelectBuilder selectBuilder) {
    return selectBuilder.column(getColumn(ID))
        .column(getColumn(NAME))
        .column(getColumn(MONTHS));
  }

  /**
   * Build the select SQL for all properties related to all loan amortizations
   * @return the SelectBuilder reference object
   */
  private SelectBuilder buildSelectSql() {
    return addColumns(new SelectBuilder(getTable()));
  }

  /**
   * Adds a join statement to the select builder provided connecting the amortization table to
   * the caller. This is the internal function
   * @param selectBuilder the select builder to add the join information to
   * @param amortizationIdColumn the column in the main table that will tie to the ID index column
   * @return the select builder returned with the modifications
   */
  private SelectBuilder joinToSelect(SelectBuilder selectBuilder, String amortizationIdColumn) {
    return addColumns(selectBuilder.join(getTable(), getColumn(ID) + "=" + amortizationIdColumn));
  }

  /*=============================================================
   * PROTECTED FUNCTIONS
   *============================================================*/

  /**
   * Updates the loan amortization info from the result set provided. This assumes it was fetched
   * appropriately by the SQL function
   * @param resultSet the result set to pull the data from. This will not call .next()
   * @throws SQLException if the data is unexpected in the result set
   */
  @Override
  protected void updateFromFetch(ResultSet resultSet) throws SQLException {
    mId = resultSet.getLong(getColumn(ID));
    mName = resultSet.getString(getColumn(NAME));
    mMonths = resultSet.getInt(getColumn(MONTHS));
  }

  /*=============================================================
   * PUBLIC FUNCTIONS
   *============================================================*/

  /**
   * Returns the API model relating to the database model
   * @return the API model for a loan amortization
   */
  public com.gncompass.serverfront.api.model.LoanAmortization getApiModel() {
    return new com.gncompass.serverfront.api.model.LoanAmortization(mId, mName, mMonths);
  }

  /**
   * Fetches the loan amortization information from the database based on the ID
   * @param amortizationId the unique amortization ID
   * @return the loan amortization object with the information fetched. If not found, return NULL
   */
  public LoanAmortization getForId(int amortizationId) {
    // Build the query
    String selectSql = buildSelectSql()
        .where(getColumn(ID) + "=" + Integer.toString(amortizationId))
        .toString();

    // Try to execute against the connection
    try (Connection conn = SQLManager.getConnection()) {
      try (ResultSet rs = conn.prepareStatement(selectSql).executeQuery()) {
        if (rs.next()) {
          updateFromFetch(rs);
          return this;
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Unable to fetch the loan amortization reference with SQL", e);
    }

    return null;
  }

  /*
   * Returns the table name of the class
   * @return the object table name
   */
  @Override
  public String getTable() {
    return TABLE_NAME;
  }

  /**
   * Returns the total number of years that the loan will last for. This can be partial values such
   * as 0.5 for a 6 month period
   * @return the total number of years. E.g. 1 for a 12 month, 0.5 for a 6 month amortization
   */
  public double getTotalYears() {
    if (mMonths > 0) {
      return (double) mMonths / PaymentHelper.MONTHS_PER_YEAR;
    }
    return 0;
  }

  /*=============================================================
   * STATIC FUNCTIONS
   *============================================================*/

  /**
   * Fetches all loan amortizations available for creating loans
   * @return the stack of loan amortizations available. Empty list if none found
   */
  public static List<LoanAmortization> getAll() {
    List<LoanAmortization> loanAmortizations = new ArrayList<>();

    // Build the query
    String selectSql = new LoanAmortization().buildSelectSql().toString();

    // Try to execute against the connection
    try (Connection conn = SQLManager.getConnection()) {
      try (ResultSet rs = conn.prepareStatement(selectSql).executeQuery()) {
        while (rs.next()) {
          loanAmortizations.add(new LoanAmortization(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Unable to fetch all the loan amortizations with SQL", e);
    }

    return loanAmortizations;
  }

  /**
   * Fetches all loan amortizations available for creating loans (in the API model form)
   * @return the stack of loan amortizations available as API model. Empty list if none found
   */
  public static List<com.gncompass.serverfront.api.model.LoanAmortization> getAllAsModel() {
    List<com.gncompass.serverfront.api.model.LoanAmortization> loanAmortizations
        = new ArrayList<>();

    // Build the query
    String selectSql = new LoanAmortization().buildSelectSql().toString();

    // Try to execute against the connection
    try (Connection conn = SQLManager.getConnection()) {
      try (ResultSet rs = conn.prepareStatement(selectSql).executeQuery()) {
        while (rs.next()) {
          loanAmortizations.add(new LoanAmortization(rs).getApiModel());
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Unable to fetch all the loan amortization models with SQL", e);
    }

    return loanAmortizations;
  }

  /**
   * Adds a join statement to the select builder provided connecting the amortization table to
   * the caller
   * @param selectBuilder the select builder to add the join information to
   * @param amortizationIdColumn the column in the main table that will tie to the ID index column
   * @return the select builder returned with the modifications
   */
  static SelectBuilder join(SelectBuilder selectBuilder, String amortizationIdColumn) {
    return new LoanAmortization().joinToSelect(selectBuilder, amortizationIdColumn);
  }
}
