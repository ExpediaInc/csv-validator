package uk.gov.tna.dri.csv.validator.api

import uk.gov.tna.dri.csv.validator.schema.{Schema, SchemaParser}
import scalaz._
import java.io.{Reader => JReader, FileReader => JFileReader}
import resource._
import scalax.file.Path
import uk.gov.tna.dri.csv.validator.{AllErrorsMetaDataValidator, FailFastMetaDataValidator, FailMessage, MetaDataValidator}

object CsvValidator {

  type PathFrom = String
  type PathTo = String
  type SubstitutePath = (PathFrom, PathTo)

  def createValidator(failFast: Boolean, pathSubstitutionsList: List[SubstitutePath]) = {
    if(failFast) {
      new CsvValidator with FailFastMetaDataValidator { val pathSubstitutions = pathSubstitutionsList }
    } else {
      new CsvValidator with AllErrorsMetaDataValidator { val pathSubstitutions = pathSubstitutionsList }
    }
  }
}

trait CsvValidator extends SchemaParser {
  this: MetaDataValidator =>

  def validate(metaDataFile: Path, schema: Schema): MetaDataValidation[Any] = {
    withReader(metaDataFile) {
      reader =>
        validate(reader, schema)
    }
  }

  def parseSchema(schemaFilePath: Path): ValidationNel[FailMessage, Schema] = {
    withReader(schemaFilePath) {
      reader =>
        parseAndValidate(reader)
    }
  }

  def parseSchema(schema: JReader): ValidationNel[FailMessage, Schema] = parseAndValidate(schema)

  private def withReader[B](file: Path)(fn: JFileReader => B): B = {
    managed(new JFileReader(file.path)).map {
      reader =>
        fn(reader)
    }.either match {
      case Left(ioError) =>
        throw ioError(0)
      case Right(result) =>
        result
    }
  }
}
