from collections.abc import Callable
from typing import Any

from google.cloud import pubsub_v1


class PubSubPullSubscriber:
    """Long-running StreamingPull adapter for local development."""

    def __init__(self, subscription_path: str) -> None:
        self.subscription_path = subscription_path

    def listen(self, callback: Callable[[Any], object]) -> None:
        flow_control = pubsub_v1.types.FlowControl(max_messages=1)
        with pubsub_v1.SubscriberClient() as subscriber:
            future = subscriber.subscribe(
                self.subscription_path,
                callback=callback,
                flow_control=flow_control,
            )
            try:
                future.result()
            except KeyboardInterrupt:
                future.cancel()
                return
