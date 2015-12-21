/*
 * Copyright 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.kubernetes.deploy.validators

import com.netflix.spinnaker.clouddriver.kubernetes.security.KubernetesCredentials
import com.netflix.spinnaker.clouddriver.kubernetes.security.KubernetesNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.security.DefaultAccountCredentialsProvider
import com.netflix.spinnaker.clouddriver.security.MapBackedAccountCredentialsRepository
import org.springframework.validation.Errors
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class StandardKubernetesAttributeValidatorSpec extends Specification {
  private static final ACCOUNT_NAME = "auto"
  private static final DECORATOR = "decorator"

  @Shared
  DefaultAccountCredentialsProvider accountCredentialsProvider

  void setupSpec() {
    def credentialsRepo = new MapBackedAccountCredentialsRepository()
    accountCredentialsProvider = new DefaultAccountCredentialsProvider(credentialsRepo)
    def namedAccountCredentialsMock = Mock(KubernetesNamedAccountCredentials)
    namedAccountCredentialsMock.getName() >> ACCOUNT_NAME
    namedAccountCredentialsMock.getCredentials() >> new KubernetesCredentials(null, null)
    credentialsRepo.save(ACCOUNT_NAME, namedAccountCredentialsMock)
  }

  void "notEmpty accept"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateNotEmpty("not-empty", label)
    then:
      0 * errorsMock._

    when:
      validator.validateNotEmpty(" ", label)
    then:
      0 * errorsMock._

    when:
      validator.validateNotEmpty([[]], label)
    then:
      0 * errorsMock._

    when:
      validator.validateNotEmpty([null], label)
    then:
      0 * errorsMock._

    when:
      validator.validateNotEmpty(0, label)
    then:
      0 * errorsMock._
  }

  @Unroll
  void "notEmpty reject"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateNotEmpty(null, label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.empty")
      0 * errorsMock._

    when:
      validator.validateNotEmpty("", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.empty")
      0 * errorsMock._

    when:
      validator.validateNotEmpty([], label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.empty")
      0 * errorsMock._
  }

  void "nonNegative accept"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateNonNegative(0, label)
    then:
      0 * errorsMock._

    when:
      validator.validateNonNegative(1, label)
    then:
      0 * errorsMock._

    when:
      validator.validateNonNegative(1 << 30, label)
    then:
      0 * errorsMock._
  }

  void "nonNegative reject"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateNonNegative(-1, label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.negative")
      0 * errorsMock._
  }

  void "byRegex accept"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"
      def pattern = /^[a-z0-9A-Z_-]{2,10}$/

    when:
      validator.validateByRegex("check-me", label, pattern)
    then:
      0 * errorsMock._

    when:
      validator.validateByRegex("1-2_3-f", label, pattern)
    then:
      0 * errorsMock._
  }

  void "byRegex reject"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"
      def pattern = /^[a-z0-9A-Z_-]{2,10}$/

    when:
      validator.validateByRegex("too-big-to-fail", label, pattern)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${pattern})")
      0 * errorsMock._

    when:
      validator.validateByRegex("1", label, pattern)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${pattern})")
      0 * errorsMock._

    when:
      validator.validateByRegex("a space", label, pattern)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${pattern})")
      0 * errorsMock._
  }

  void "credentials reject (empty)"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)

    when:
      validator.validateCredentials(null, accountCredentialsProvider)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.credentials", "${DECORATOR}.credentials.empty")
      0 * errorsMock._

    when:
      validator.validateCredentials("", accountCredentialsProvider)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.credentials", "${DECORATOR}.credentials.empty")
      0 * errorsMock._
  }

  void "credentials reject (unknown)"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)

    when:
      validator.validateCredentials("You-don't-know-me", accountCredentialsProvider)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.credentials", "${DECORATOR}.credentials.notFound")
      0 * errorsMock._
  }

  void "credentials accept"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)

    when:
      validator.validateCredentials(ACCOUNT_NAME, accountCredentialsProvider)
    then:
      0 * errorsMock._
  }

  void "details accept"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateDetails("valid", label)
    then:
      0 * errorsMock._

    when:
      validator.validateDetails("also-valid", label)
    then:
      0 * errorsMock._

    when:
      validator.validateDetails("123-456-789", label)
    then:
      0 * errorsMock._

    when:
      validator.validateDetails("", label)
    then:
      0 * errorsMock._
  }

  void "details reject"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateDetails("-", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.namePattern})")
      0 * errorsMock._ 

    when:
      validator.validateDetails("a space", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.namePattern})")
      0 * errorsMock._

    when:
      validator.validateDetails("bad*details", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.namePattern})")
      0 * errorsMock._

    when:
      validator.validateDetails("-k-e-b-a-b-", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.namePattern})")
      0 * errorsMock._ 
  }

  void "name accept"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateName("valid", label)
    then:
      0 * errorsMock._

    when:
      validator.validateName("mega-valid-name", label)
    then:
      0 * errorsMock._

    when:
      validator.validateName("call-me-123-456-7890", label)
    then:
      0 * errorsMock._
  }

  void "name reject"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateName("-", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.namePattern})")
      0 * errorsMock._ 

    when:
      validator.validateName("an_underscore", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.namePattern})")
      0 * errorsMock._

    when:
      validator.validateName("?name", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.namePattern})")
      0 * errorsMock._

    when:
      validator.validateName("", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.empty")
      0 * errorsMock._
  }

  void "application accept"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateApplication("valid", label)
    then:
      0 * errorsMock._

    when:
      validator.validateApplication("application", label)
    then:
      0 * errorsMock._

    when:
      validator.validateApplication("7890", label)
    then:
      0 * errorsMock._
  }

  void "application reject"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateApplication("l-l", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.prefixPattern})")
      0 * errorsMock._ 

    when:
      validator.validateApplication("?application", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.prefixPattern})")
      0 * errorsMock._

    when:
      validator.validateApplication("", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.empty")
      0 * errorsMock._
  }

  void "stack accept"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateStack("valid", label)
    then:
      0 * errorsMock._

    when:
      validator.validateStack("stack", label)
    then:
      0 * errorsMock._

    when:
      validator.validateStack("7890", label)
    then:
      0 * errorsMock._
  }

  void "stack reject"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateStack("l-l", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.prefixPattern})")
      0 * errorsMock._ 

    when:
      validator.validateStack("?stack", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.prefixPattern})")
      0 * errorsMock._

    when:
      validator.validateStack("", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.empty")
      0 * errorsMock._
  }

  void "memory accept"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateMemory("", label)
    then:
      0 * errorsMock._

    when:
      validator.validateMemory("100Mi", label)
    then:
      0 * errorsMock._

    when:
      validator.validateMemory("1Gi", label)
    then:
      0 * errorsMock._
  }

  void "memory reject"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateMemory("100", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.memoryPattern})")
      0 * errorsMock._ 

    when:
      validator.validateMemory("x100Gi", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.memoryPattern})")
      0 * errorsMock._

    when:
      validator.validateMemory("1Ti", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.memoryPattern})")
      0 * errorsMock._
  }

  void "cpu accept"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateCpu("", label)
    then:
      0 * errorsMock._

    when:
      validator.validateCpu("100m", label)
    then:
      0 * errorsMock._

    when:
      validator.validateCpu("2m", label)
    then:
      0 * errorsMock._
  }

  void "cpu reject"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardKubernetesAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateCpu("100", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.cpuPattern})")
      0 * errorsMock._ 

    when:
      validator.validateCpu("10M", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.cpuPattern})")
      0 * errorsMock._

    when:
      validator.validateCpu("1G", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardKubernetesAttributeValidator.cpuPattern})")
      0 * errorsMock._
  }
}
