package es.upv.mist.cauder.model

import com.ericsson.otp.erlang.OtpErlangObject
import es.upv.mist.cauder.erlang.MFA
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.SimpleListProperty
import tornadofx.*

class CauderProcess(
    pid: Int,
    environment: List<Binding>,
    expressions: List<OtpErlangObject>,
    entryPoint: MFA,
) {
    private val _pidProp = ReadOnlyIntegerWrapper(pid)
    val pidProp = _pidProp.readOnlyProperty
    val pid by pidProp

    val environmentProp = SimpleListProperty(environment.asObservable())
    val environment by environmentProp

    val expressionsProp = SimpleListProperty(expressions.asObservable())
    val expressions by expressionsProp

    val entryPoint: MFA = entryPoint
}
