import connexion
import six

from swagger_server.models.body import Body  # noqa: E501
from swagger_server.models.inline_response200 import InlineResponse200  # noqa: E501
from swagger_server import util


def pollution_put(body):  # noqa: E501
    """Inserimento dati sulla qualità dell&#39;aria

    Permette di inserire dati sulla qualità dell&#39;aria nel database # noqa: E501

    :param body: Dati sulla qualità dell&#39;aria
    :type body: dict | bytes

    :rtype: InlineResponse200
    """
    if connexion.request.is_json:
        body = Body.from_dict(connexion.request.get_json())  # noqa: E501
    return 'do some magic!'
