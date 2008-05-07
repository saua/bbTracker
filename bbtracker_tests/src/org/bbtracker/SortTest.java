package org.bbtracker;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class SortTest {

	static final Comparator MY_COMPARATOR = new Comparator() {

		public int compare(Object a, Object b) {
			double ia = ((Number) a).intValue();
			double ib = ((Number) b).intValue();
			if (ia < ib) {
				return -1;
			} else if (ia > ib) {
				return 1;
			} else {
				return 0;
			}
		}
	};

	@Test
	public void sortEmptyList() {
		checkSort(new Object[] {}, new Object[] {});
	}

	@Test
	public void sortSingleElement() {
		checkSort(new Object[] { 1 }, new Object[] { 1 });
	}

	@Test
	public void sortTwoSorted() {
		checkSort(new Object[] { 1, 2 }, new Object[] { 1, 2 });
	}

	@Test
	public void sortTwoUnsorted() {
		checkSort(new Object[] { 2, 1 }, new Object[] { 1, 2 });
	}

	@Test
	public void sortTwoThreeSorted() {
		checkSort(new Object[] { 1, 2, 3 }, new Object[] { 1, 2, 3 });
	}

	@Test
	public void sortTwoThree1() {
		checkSort(new Object[] { 2, 3, 1 }, new Object[] { 1, 2, 3 });
	}

	@Test
	public void sortTwoThree2() {
		checkSort(new Object[] { 3, 1, 2 }, new Object[] { 1, 2, 3 });
	}

	@Test
	public void sortTwoThree3() {
		checkSort(new Object[] { 3, 2, 1 }, new Object[] { 1, 2, 3 });
	}

	@Test
	public void sortTenSorted() {
		checkSort(new Object[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, new Object[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
	}

	@Test
	public void sortTen1() {
		checkSort(new Object[] { 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 }, new Object[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
	}

	@Test
	public void sortTen2() {
		checkSort(new Object[] { 1, 9, 6, 5, 10, 8, 4, 3, 7, 2 }, new Object[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
	}

	@Test
	public void sortTen3() {
		checkSort(new Object[] { 1, 2, 8, 4, 5, 6, 7, 3, 9, 10 }, new Object[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
	}

	private void checkSort(final Object[] in, final Object[] out) {
		Utils.quicksort(in, MY_COMPARATOR);
		assertArrayEquals(out, in);
	}
}
