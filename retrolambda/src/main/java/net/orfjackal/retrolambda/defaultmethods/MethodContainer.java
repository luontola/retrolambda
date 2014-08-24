// Copyright © 2013-2014 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda.defaultmethods;

import java.util.Arrays;

/**
* Created by arneball on 2014-08-23.
*/
class MethodContainer {
	public final String methodName, methodDesc, interfce, signature;
	public final String[] exceptions;

	@Override
	public String toString() {
		return "MethodContainer{" +
				"methodName='" + methodName + '\'' +
				", methodDesc='" + methodDesc + '\'' +
				", interfce='" + interfce + '\'' +
				", signature='" + signature + '\'' +
				", exceptions=" + Arrays.toString(exceptions) +
				'}';
	}

	MethodContainer(String methodName, String methodDesc, String interfce, String signature, String[] exceptions) {
		this.methodName = methodName;
		this.methodDesc = methodDesc;
		this.interfce = interfce;
		this.signature = signature;
		this.exceptions = exceptions;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MethodContainer that = (MethodContainer) o;

		if (!Arrays.equals(exceptions, that.exceptions)) return false;
		if (methodDesc != null ? !methodDesc.equals(that.methodDesc) : that.methodDesc != null) return false;
		if (methodName != null ? !methodName.equals(that.methodName) : that.methodName != null) return false;
		if (signature != null ? !signature.equals(that.signature) : that.signature != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = methodName != null ? methodName.hashCode() : 0;
		result = 31 * result + (methodDesc != null ? methodDesc.hashCode() : 0);
		result = 31 * result + (signature != null ? signature.hashCode() : 0);
		result = 31 * result + (exceptions != null ? Arrays.hashCode(exceptions) : 0);
		return result;
	}
}
