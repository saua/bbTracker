package org.bbtracker;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.StringReader;

import org.bbtracker.CsvReader.MalformedCsvException;
import org.junit.Test;

public class CsvReaderTest {
	private static CsvReader newReader(final String s) {
		return new CsvReader(new StringReader(s));
	}

	@Test
	public void empty() throws IOException {
		assertThat(newReader("").nextLine(), is(nullValue()));
	}

	@Test
	public void twoEmptyLines() throws IOException {
		final CsvReader reader = newReader("\r\n");
		assertThat(reader.nextLine(), is(new String[0]));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void simpleLine() throws IOException {
		final CsvReader reader = newReader("1,foo,bar");
		assertThat(reader.nextLine(), is(new String[] { "1", "foo", "bar" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void simpleLineWithCRLF() throws IOException {
		final CsvReader reader = newReader("1,foo,bar\r\n");
		assertThat(reader.nextLine(), is(new String[] { "1", "foo", "bar" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void simpleQuotedField() throws IOException {
		final CsvReader reader = newReader("\"foo\"");
		assertThat(reader.nextLine(), is(new String[] { "foo" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void simpleQuotedFieldFollowedByNormalField() throws IOException {
		final CsvReader reader = newReader("\"foo\",1");
		assertThat(reader.nextLine(), is(new String[] { "foo", "1" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void simpleQuotedFieldFollowedByCRLF() throws IOException {
		final CsvReader reader = newReader("\"foo\"\r\n1");
		assertThat(reader.nextLine(), is(new String[] { "foo" }));
		assertThat(reader.nextLine(), is(new String[] { "1" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void twoSimpleQuotedFields() throws IOException {
		final CsvReader reader = newReader("\"foo\",\"bar\"");
		assertThat(reader.nextLine(), is(new String[] { "foo", "bar" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void quotedFieldWithQuotes() throws IOException {
		final CsvReader reader = newReader("\"foo\"\"bar\"");
		assertThat(reader.nextLine(), is(new String[] { "foo\"bar" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void quotedFieldWithQuotesFollowedBySimpleField() throws IOException {
		final CsvReader reader = newReader("\"foo\"\"bar\",1");
		assertThat(reader.nextLine(), is(new String[] { "foo\"bar", "1" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void quotedFieldWithQuotesFollowedByCRLF() throws IOException {
		final CsvReader reader = newReader("\"foo\"\"bar\"\r\n1");
		assertThat(reader.nextLine(), is(new String[] { "foo\"bar" }));
		assertThat(reader.nextLine(), is(new String[] { "1" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void quotedFieldWithCR() throws IOException {
		final CsvReader reader = newReader("\"foo\rbar\"");
		assertThat(reader.nextLine(), is(new String[] { "foo\rbar" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void quotedFieldWithLF() throws IOException {
		final CsvReader reader = newReader("\"foo\nbar\"");
		assertThat(reader.nextLine(), is(new String[] { "foo\nbar" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void quotedFieldWithCRLF() throws IOException {
		final CsvReader reader = newReader("\"foo\r\nbar\"");
		assertThat(reader.nextLine(), is(new String[] { "foo\r\nbar" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void quotedFieldWithComma() throws IOException {
		final CsvReader reader = newReader("\"foo,bar\",baz");
		assertThat(reader.nextLine(), is(new String[] { "foo,bar", "baz" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void emptyFieldFirst() throws IOException {
		final CsvReader reader = newReader(",foo");
		assertThat(reader.nextLine(), is(new String[] { "", "foo" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void emptyFieldLast() throws IOException {
		final CsvReader reader = newReader("foo,");
		assertThat(reader.nextLine(), is(new String[] { "foo", "" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void emptyFieldBetweenFields() throws IOException {
		final CsvReader reader = newReader("foo,,bar");
		assertThat(reader.nextLine(), is(new String[] { "foo", "", "bar" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void emptyFieldBeforeCRLF() throws IOException {
		final CsvReader reader = newReader("foo,\r\nbar");
		assertThat(reader.nextLine(), is(new String[] { "foo", "" }));
		assertThat(reader.nextLine(), is(new String[] { "bar" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void emptyFieldAfterCRLF() throws IOException {
		final CsvReader reader = newReader("foo\r\n,bar");
		assertThat(reader.nextLine(), is(new String[] { "foo" }));
		assertThat(reader.nextLine(), is(new String[] { "", "bar" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void onlyEmptyFields() throws IOException {
		final CsvReader reader = newReader(",,");
		assertThat(reader.nextLine(), is(new String[] { "", "", "" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void onlyEmptyFieldsTwoLines() throws IOException {
		final CsvReader reader = newReader(",,\r\n,,");
		assertThat(reader.nextLine(), is(new String[] { "", "", "" }));
		assertThat(reader.nextLine(), is(new String[] { "", "", "" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test
	public void quotedFieldWithSpace() throws IOException {
		final CsvReader reader = newReader("foo, \"bar\", baz");
		assertThat(reader.nextLine(), is(new String[] { "foo", "bar", " baz" }));
		assertThat(reader.nextLine(), is(nullValue()));
	}

	@Test(expected = MalformedCsvException.class)
	public void newlineWithCROnly() throws IOException {
		newReader("foo\rbar").nextLine();
	}

	@Test(expected = MalformedCsvException.class)
	public void newlineWithCROnlyAfterQuotedField() throws IOException {
		newReader("foo\rbar").nextLine();
	}

	@Test(expected = MalformedCsvException.class)
	public void unclosedQuote() throws IOException {
		newReader("\"foo").nextLine();
	}

	@Test(expected = MalformedCsvException.class)
	public void quoteInField() throws IOException {
		newReader("foo\"bar").nextLine();
	}
}