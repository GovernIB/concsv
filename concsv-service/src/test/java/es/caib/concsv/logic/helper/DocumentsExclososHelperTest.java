package es.caib.concsv.logic.helper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests de {@link DocumentsExclososHelper}: consulta dels identificadors a la base de dades i
 * tractament com a exclòs si la base de dades falla.
 */
public class DocumentsExclososHelperTest {

	private DocumentsExclososHelper helper;
	private DataSource dataSource;
	private Connection connection;
	private PreparedStatement statement;
	private ResultSet resultSet;
	private SubsistemesHelper subsistemesHelper;

	@Before
	public void setUp() throws Exception {
		dataSource = mock(DataSource.class);
		connection = mock(Connection.class);
		statement = mock(PreparedStatement.class);
		resultSet = mock(ResultSet.class);
		subsistemesHelper = mock(SubsistemesHelper.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(resultSet);
		helper = new DocumentsExclososHelper();
		helper.setDataSource(dataSource);
		helper.setSubsistemesHelper(subsistemesHelper);
	}

	@Test
	public void isExclos_siLaBdElTroba() throws Exception {
		when(resultSet.next()).thenReturn(true);

		assertTrue(helper.isExclos("uuid-1", "csv-1"));

		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(connection).prepareStatement(sql.capture());
		assertTrue(sql.getValue().contains("VALOR IN (?, ?)"));
		verify(statement).setString(1, "uuid-1");
		verify(statement).setString(2, "csv-1");
	}

	@Test
	public void isExclos_siLaBdNoElTroba() throws Exception {
		when(resultSet.next()).thenReturn(false);

		assertFalse(helper.isExclos("uuid-1", "csv-1"));
	}

	@Test
	public void isExclos_ignoraNulsBuitsIRepetitsISenseIdentificadorsNoConsultaLaBd() throws Exception {
		assertFalse(helper.isExclos());
		assertFalse(helper.isExclos((String) null, "  ", ""));
		assertFalse(helper.isExclos((String[]) null));
		verify(dataSource, never()).getConnection();

		when(resultSet.next()).thenReturn(false);
		helper.isExclos(null, " csv-1 ", "csv-1");
		verify(statement).setString(1, "csv-1");
		verify(statement, never()).setString(2, "csv-1");
	}

	@Test
	public void isExclos_siLaBdFallaEsConsideraExclosIRegistraL_error() throws Exception {
		when(dataSource.getConnection()).thenThrow(new SQLException("ORA-00942"));

		assertTrue(helper.isExclos("csv-1"));
		verify(subsistemesHelper).addOperation(
				org.mockito.ArgumentMatchers.eq(SubsistemesHelper.SubsistemesEnum.EXC),
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.eq(true));
	}

}
