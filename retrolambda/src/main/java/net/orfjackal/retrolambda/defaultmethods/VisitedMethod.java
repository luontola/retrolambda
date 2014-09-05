// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.defaultmethods;

/**
* Created by arneball on 2014-08-23.
*/
class VisitedMethod {
	public final String name, desc;

	VisitedMethod(String name, String desc) {
		this.name = name;
		this.desc = desc;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		VisitedMethod that = (VisitedMethod) o;

		if (!desc.equals(that.desc)) return false;
		if (!name.equals(that.name)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + desc.hashCode();
		return result;
	}
}
