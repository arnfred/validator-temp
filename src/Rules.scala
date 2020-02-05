package validator
import org.json4s._

sealed abstract trait Rule {
  val path: Seq[String]
  val key: String = path.mkString(".")
  def value(json: JValue): JValue = path.foldLeft(json)((jvalue, key) => jvalue \ key)
  def validate(json: JValue): Option[String]
}

case class MinRule(path: Seq[String], threshold: BigInt) extends Rule {
  def validate(json: JValue) = value(json) match {
    case JInt(n) if (n < threshold) => Some(s"Key ${key} must be more than $threshold. Currently $n")
    case _ => None
  }
}

case class MaxRule(path: Seq[String], threshold: BigInt) extends Rule {
  def validate(json: JValue) = value(json) match {
    case JInt(n) if (n > threshold) => Some(s"Key ${key} must be less than $threshold. Currently $n")
    case _ => None
  }
}

case class TypeIntRule(path: Seq[String]) extends Rule {
  def validate(json: JValue) = value(json) match {
    case JInt(_) => None
    case _ => Some(s"Key ${key} must be an integer")
  }
}

case class TypeStringRule(path: Seq[String]) extends Rule {
  def validate(json: JValue) = value(json) match {
    case JString(_) => None
    case _ => Some(s"Key ${key} must be a string")
  }
}

case class TypeObjectRule(path: Seq[String]) extends Rule {
  def validate(json: JValue) = value(json) match {
    case JObject(n) => None
    case _ => Some(s"Key ${key} must be an object")
  }
}

case class RequiredRule(path: Seq[String], field: String) extends Rule {
  val requiredKey = (path :+ field).mkString(".")
  def validate(json: JValue) = value(json) \ field match {
    case JNothing => Some(s"required key '$requiredKey' is missing")
    case _ => None
  }
}

// This code is brittle and assumes a well-formed schema
object Rules {
  def parse(json: JValue): Rules = Rules(pathsP(json, Seq()))

  private def pathsP(json: JValue, path: Seq[String]): Seq[Rule] = {
    (path.lastOption, json) match {
      case (_, JObject(values)) => values.flatMap { case (k, v) => pathsP(v, path :+ k) }
      case (Some("minimum"), JInt(value)) => Seq(MinRule(clean(path), value))
      case (Some("maximum"), JInt(value)) => Seq(MaxRule(clean(path), value))
      case (Some("type"), JString("integer")) => Seq(TypeIntRule(clean(path)))
      case (Some("type"), JString("string")) => Seq(TypeStringRule(clean(path)))
      case (Some("type"), JString("object")) => Seq(TypeObjectRule(clean(path)))
      case (Some("required"), JArray(values)) => values.collect { case JString(k) => RequiredRule(clean(path), k) }
      case (Some("$schema"), _) => Seq()
      case _ => ??? // Use case not supported
    }
  }

  private def clean(path: Seq[String]): Seq[String] = path match {
    case "properties" +: key +: rest => key +: clean(rest)
    case "type" +: Seq() => Seq()
    case "required" +: Seq() => Seq()
    case "minimum" +: Seq() => Seq()
    case "maximum" +: Seq() => Seq()
    case Seq() => Seq()
    case _ => ??? // Use case not supported
  }
}

case class Rules(rules: Seq[Rule]) {
  def validate(json: JValue): Seq[Option[String]] = rules.map(_.validate(json))
}

