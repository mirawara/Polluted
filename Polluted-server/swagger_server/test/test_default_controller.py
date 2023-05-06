# coding: utf-8

from __future__ import absolute_import

from flask import json

from swagger_server.models.body import Body  # noqa: E501
from swagger_server.test import BaseTestCase


class TestDefaultController(BaseTestCase):
    """DefaultController integration test stubs"""

    def test_pollution_put(self):
        """Test case for pollution_put

        Inserimento dati sulla qualità dell'aria
        """
        body = Body()
        response = self.client.open(
            '//pollution',
            method='PUT',
            data=json.dumps(body),
            content_type='application/json')
        self.assert200(response,
                       'Response body is : ' + response.data.decode('utf-8'))


if __name__ == '__main__':
    import unittest

    unittest.main()
