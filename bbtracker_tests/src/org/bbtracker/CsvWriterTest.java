package org.bbtracker;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class CsvWriterTest {
	private CsvWriter writer;

	@Before
	public void setup() {
		writer = new CsvWriter();
	}

	@Test
	public void checkEmpty() {
		assertEquals("", writer.toString());
	}

	@Test
	public void checkNewline() {
		assertEquals("\r\n", writer.nl().toString());
	}

	@Test
	public void checkSingleField() {
		assertEquals("foo", writer.append("foo").toString());
	}

	@Test
	public void checkSingleFieldTwoLines() {
		assertEquals("foo\r\nbar", writer.append("foo").nl().append("bar").toString());
	}

	@Test
	public void checkTwoFields() {
		assertEquals("foo,bar", writer.append("foo").append("bar").toString());
	}

	@Test
	public void checkTwoFieldsTwoLines() {
		assertEquals("foo,bar\r\nbaz,quux", writer.append("foo").append("bar").nl().append("baz").append("quux")
				.toString());
	}

	@Test
	public void checkMultiAppendTwoLines() {
		assertEquals("foo,bar\r\nbaz,quux", writer.append(new Object[] { "foo", "bar" }).nl().append(
				new Object[] { "baz", "quux" }).toString());
	}

	@Test
	public void checkMultiAppendNonStrings() {
		assertEquals("1,true,null", writer.append(new Object[] { Integer.valueOf(1), Boolean.TRUE, null }).toString());
	}

	@Test
	public void checkSpaceInField() {
		assertEquals("\"foo bar\"", writer.append("foo bar").toString());
	}

	@Test
	public void checkNewlineInField() {
		assertEquals("\"foo\nbar\"", writer.append("foo\nbar").toString());
	}

	@Test
	public void checkQuoteInField() {
		assertEquals("\"foo\"\"bar\"", writer.append("foo\"bar").toString());
	}

	@Test
	public void checkQuoteAroundField() {
		assertEquals("\"\"\"foo\"\"\"", writer.append("\"foo\"").toString());
	}
}
